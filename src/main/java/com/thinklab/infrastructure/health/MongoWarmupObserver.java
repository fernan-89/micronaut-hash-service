package com.thinklab.infrastructure.health;

import com.mongodb.ConnectionString;
import com.mongodb.reactivestreams.client.MongoClient;
import io.micronaut.context.ApplicationContext;
import io.micronaut.context.annotation.Property;
import io.micronaut.context.event.ApplicationEventListener;
import io.micronaut.context.event.StartupEvent;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import org.bson.BsonDocument;
import org.bson.BsonInt32;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.Objects;

/**
 * Infrastructure Component: Proactively initializes the MongoDB Reactive Driver connection pool.
 *
 * <p><b>Architectural Role:</b>
 * By executing a deterministic 'ping' command upon application startup, this observer forces the
 * Server Discovery and Monitoring (SDAM) mechanism to resolve the cluster topology before Kubernetes
 * Readiness Probes poll the health endpoint. This eliminates transient 'UNKNOWN' states and cold-start latency.
 *
 * <p><b>Two-Phase Verification:</b>
 * <ol>
 * <li><b>Cluster Health:</b> Validates global connectivity via the 'admin' database.</li>
 * <li><b>Application Context:</b> Verifies RBAC and connectivity against the specific application database dynamically extracted from the URI.</li>
 * </ol>
 *
 * <p><b>Resilience & Circuit Breaker (ADR-002):</b>
 * If the database is unreachable, this component acts as a passive Circuit Breaker. The application context
 * will start, but the readiness probe will fail, preventing traffic routing. It will then attempt a
 * progressive backoff retry strategy (15s, 30s, 60s). If all attempts are exhausted, it will gracefully
 * shut down the application context, delegating the pod restart to the container orchestration layer (Kubernetes).
 *
 * <p><b>Synchronous Barrier (ADR-007):</b>
 * During the {@link StartupEvent}, this component intentionally blocks the main initialization thread
 * using Reactor's blocking operators. This guarantees the application does not report as "Running"
 * and does not serve HTTP traffic until the database topology is fully resolved or the fail-fast shutdown is triggered.
 *
 * <p><b>Contractual Obligations:</b>
 * <ul>
 * <li><b>Constructor Injection (ADR-001):</b> Utilizes an explicit {@link Inject} constructor to guarantee
 *     deterministic bean wiring and AOP proxy reliability.</li>
 * <li><b>Proactive Connection Warm-Up:</b> Establishes database socket connectivity synchronously on startup.</li>
 * <li><b>Structured Telemetry & Observability:</b> Emits latency metrics and standardized diagnostic markers for centralized logging.</li>
 * </ul>
 *
 * @author ThinkLab
 * @version 2.7.0
 * @since 1.0
 */
@Singleton
@Slf4j
public class MongoWarmupObserver implements ApplicationEventListener<StartupEvent> {

    private final MongoClient mongoClient;
    private final String applicationDatabase;
    private final ApplicationContext applicationContext;

    /**
     * Explicit constructor for strict dependency injection (ADR-001) and property binding.
     * Extracts the target database dynamically from the MongoDB URI to avoid configuration duplication.
     * Includes a failsafe to prevent malformed environment variables from crashing the context.
     *
     * @param mongoClient        The reactive MongoDB client driver instance. Must not be null.
     * @param mongoUri           The full MongoDB connection string injected from application.yml.
     * @param applicationContext The Micronaut application context for orchestrating graceful shutdowns.
     * @throws NullPointerException if mandatory dependencies are missing.
     */
    @Inject
    public MongoWarmupObserver(
            MongoClient mongoClient,
            @Property(name = "mongodb.uri") String mongoUri,
            ApplicationContext applicationContext
    ) {
        this.mongoClient = Objects.requireNonNull(mongoClient, "Application constraint violated: MongoClient cannot be null.");
        this.applicationContext = Objects.requireNonNull(applicationContext, "Application constraint violated: ApplicationContext cannot be null.");

        String parsedDatabase = null;

        try {
            // Dynamically parse the URI to extract the target database, preventing configuration drift
            ConnectionString connectionString = new ConnectionString(mongoUri);
            parsedDatabase = connectionString.getDatabase();
        } catch (IllegalArgumentException e) {
            // Failsafe: Prevents malformed URIs from crashing the application context during bean instantiation.
            // The passive circuit breaker will handle the actual connection failure downstream.
            log.warn("[MONGODB_WARMUP] ⚠️ URI Parse Error: Failed to extract target database dynamically. Reason: {}. Falling back to 'admin'...", e.getMessage());
        }

        // Graceful fallback to 'admin' if the URI lacks a specific database path or parsing fails.
        this.applicationDatabase = (parsedDatabase != null && !parsedDatabase.isBlank()) ? parsedDatabase : "admin";
    }

    /**
     * Intercepts the framework's StartupEvent to proactively initialize the MongoDB connection pool.
     * Constructs a BSON ping command and dispatches it sequentially. Includes a resilient retry policy
     * and a final terminal signal that stops the container if the database is definitively unreachable.
     * Implements a Synchronous Barrier (ADR-007) to prevent premature traffic routing.
     *
     * @param event The application startup event triggered by the IoC container. Must not be null.
     */
    @Override
    public void onApplicationEvent(StartupEvent event) {
        Objects.requireNonNull(event, "Application constraint violated: StartupEvent cannot be null.");

        log.info("[MONGODB_WARMUP] ➔ Initializing SDAM topology discovery... [component=mongodb | status=INIT]");
        log.debug("[MONGODB_WARMUP] ➔ Target database dynamically resolved from URI: [{}]", applicationDatabase);

        BsonDocument pingCommand = new BsonDocument("ping", new BsonInt32(1));
        long startTime = System.currentTimeMillis();

        try {
            // Phase 1: Cluster Warmup via Admin DB
            Mono.from(mongoClient.getDatabase("admin").runCommand(pingCommand))
                    .doOnSuccess(adminRes -> log.debug("[MONGODB_WARMUP] ✔ Cluster connectivity verified. Checking application database: [{}]", applicationDatabase))

                    // Phase 2: Contextual verification via Application DB
                    .flatMap(adminRes -> Mono.from(mongoClient.getDatabase(applicationDatabase).runCommand(pingCommand)))

                    // Enforce a strict network boundary (5s max wait per attempt)
                    .timeout(Duration.ofSeconds(5))

                    // --- PROGRESSIVE RETRY POLICY ---
                    .retryWhen(Retry.from(retrySignals -> retrySignals.flatMap(rs -> {
                        long attempt = rs.totalRetries();
                        Throwable error = rs.failure();

                        if (attempt == 0) {
                            log.warn("[MONGODB_CIRCUIT_BREAKER] ⚠️ Attempt 1 failed. Pod is UNREADY. Retrying in 15s... [error={}]", error.getMessage());
                            return Mono.delay(Duration.ofSeconds(15));
                        } else if (attempt == 1) {
                            log.warn("[MONGODB_CIRCUIT_BREAKER] ⚠️ Attempt 2 failed. Pod is UNREADY. Retrying in 30s... [error={}]", error.getMessage());
                            return Mono.delay(Duration.ofSeconds(30));
                        } else if (attempt == 2) {
                            log.warn("[MONGODB_CIRCUIT_BREAKER] ⚠️ Attempt 3 failed. Pod is UNREADY. Retrying in 60s... [error={}]", error.getMessage());
                            return Mono.delay(Duration.ofSeconds(60));
                        }

                        // Circuit broken: all resilient retries exhausted.
                        log.error("[MONGODB_CIRCUIT_BREAKER] 🚨 Exhausted all connection retry attempts (Total wait: 105s).");
                        return Mono.error(error);
                    })))

                    // Success Telemetry
                    .doOnSuccess(appResult -> {
                        long duration = System.currentTimeMillis() - startTime;
                        log.info("[MONGODB_WARMUP] ✔ Telemetry established successfully. [component=mongodb | status=UP | targetDatabase={} | latency={}ms]",
                                applicationDatabase, duration);
                    })

                    // Terminal Failure Handling
                    .doOnError(error -> {
                        long duration = System.currentTimeMillis() - startTime;
                        log.error("[MONGODB_WARMUP] ✖ CRITICAL: Topology discovery definitively failed! Initiating container shutdown. [component=mongodb | status=DOWN | targetDatabase={} | latency={}ms | cause='{}']",
                                applicationDatabase, duration, error.getMessage(), error);

                        // Gracefully stops the IoC container, resulting in a SIGTERM for Kubernetes to handle
                        applicationContext.stop();
                    })

                    // ADR-007: Synchronous barrier ensuring no HTTP traffic is served before checks complete.
                    // Accommodates the 105 seconds of progressive retry delays + connection timeouts.
                    .block(Duration.ofSeconds(120));

            log.debug("[MONGODB_WARMUP] Warmup lifecycle completed successfully.");

        } catch (Exception e) {
            log.error("[MONGODB_WARMUP] 🛑 Synchronous barrier caught terminal error. Awaiting context shutdown...");
        }
    }
}