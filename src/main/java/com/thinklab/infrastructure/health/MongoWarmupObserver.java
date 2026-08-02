package com.thinklab.infrastructure.health;

import com.mongodb.reactivestreams.client.MongoClient;
import io.micronaut.context.annotation.Property;
import io.micronaut.context.event.ApplicationEventListener;
import io.micronaut.context.event.StartupEvent;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import org.bson.BsonDocument;
import org.bson.BsonInt32;
import reactor.core.publisher.Mono;

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
 * <p>Furthermore, it implements a Two-Phase Verification approach:
 * <ol>
 * <li><b>Cluster Health:</b> Validates global connectivity via the 'admin' database.</li>
 * <li><b>Application Context:</b> Verifies RBAC and connectivity against the specific application database.</li>
 * </ol>
 *
 * <p><b>Contractual Obligations:</b>
 * <ul>
 * <li><b>Constructor Injection (ADR-001):</b> Utilizes an explicit {@link Inject} constructor to guarantee
 *     deterministic bean wiring and AOP proxy reliability.</li>
 * <li><b>Proactive Connection Warm-Up:</b> Establishes database socket connectivity asynchronously via Project Reactor.</li>
 * <li><b>Structured Telemetry & Observability:</b> Emits latency metrics and standardized diagnostic markers for centralized logging.</li>
 * </ul>
 *
 * @author ThinkLab
 * @version 2.2.0
 * @since 1.0
 */
@Singleton
@Slf4j
public class MongoWarmupObserver implements ApplicationEventListener<StartupEvent> {

    private final MongoClient mongoClient;
    private final String applicationDatabase;

    /**
     * Explicit constructor for strict dependency injection (ADR-001) and property binding.
     *
     * @param mongoClient         The reactive MongoDB client driver instance. Must not be null.
     * @param applicationDatabase The logical name of the application's target database.
     *                            Defaults to 'unknown' if missing to prevent startup crash, but will fail the probe.
     * @throws NullPointerException if the mongoClient is null.
     */
    @Inject
    public MongoWarmupObserver(
            MongoClient mongoClient,
            @Property(name = "mongodb.database", defaultValue = "unknown") String applicationDatabase
    ) {
        this.mongoClient = Objects.requireNonNull(mongoClient, "Application constraint violated: MongoClient cannot be null.");
        this.applicationDatabase = applicationDatabase;
    }

    /**
     * Intercepts the framework's StartupEvent to proactively initialize the MongoDB connection pool.
     * Constructs a BSON ping command and dispatches it sequentially: first to the admin database
     * to warm up SDAM, then to the application database to verify contextual access.
     *
     * @param event The application startup event triggered by the IoC container. Must not be null.
     */
    @Override
    public void onApplicationEvent(StartupEvent event) {
        Objects.requireNonNull(event, "Application constraint violated: StartupEvent cannot be null.");

        log.info("[MONGODB_WARMUP] ➔ Initializing SDAM topology discovery... [component=mongodb | status=INIT]");

        BsonDocument pingCommand = new BsonDocument("ping", new BsonInt32(1));
        long startTime = System.currentTimeMillis();

        // Phase 1: Cluster Warmup via Admin DB
        Mono.from(mongoClient.getDatabase("admin").runCommand(pingCommand))
                .doOnSuccess(adminRes -> log.debug("[MONGODB_WARMUP] ✔ Cluster connectivity verified. Checking application database: [{}]", applicationDatabase))
                // Phase 2: Contextual verification via Application DB
                .flatMap(adminRes -> Mono.from(mongoClient.getDatabase(applicationDatabase).runCommand(pingCommand)))
                .timeout(Duration.ofSeconds(5))
                .doOnSuccess(appResult -> {
                    long duration = System.currentTimeMillis() - startTime;
                    log.info("[MONGODB_WARMUP] ✔ Telemetry established successfully. [component=mongodb | status=UP | targetDatabase={} | latency={}ms | response={}]",
                            applicationDatabase, duration, appResult.toJson());
                })
                .doOnError(error -> {
                    long duration = System.currentTimeMillis() - startTime;
                    log.error("[MONGODB_WARMUP] ✖ CRITICAL: Topology discovery or Database access failed! [component=mongodb | status=DOWN | targetDatabase={} | latency={}ms | cause='{}']",
                            applicationDatabase, duration, error.getMessage(), error);
                })
                .subscribe();
    }
}