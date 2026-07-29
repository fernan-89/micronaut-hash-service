package com.thinklab.infrastructure.health;

import com.mongodb.reactivestreams.client.MongoClient;
import io.micronaut.context.event.ApplicationEventListener;
import io.micronaut.context.event.StartupEvent;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import org.bson.BsonDocument;
import org.bson.BsonInt32;
import reactor.core.publisher.Mono;

import java.util.Objects;

/**
 * Infrastructure Component: Proactively initializes the MongoDB Reactive Driver connection pool.
 *
 * <p><b>Architectural Role:</b>
 * By executing a deterministic 'ping' command upon application startup, this observer forces the
 * Server Discovery and Monitoring (SDAM) mechanism to resolve the cluster topology before Kubernetes
 * Readiness Probes poll the health endpoint. This eliminates transient 'UNKNOWN' states and cold-start latency.
 *
 * <p><b>Contractual Obligations:</b>
 * <ul>
 * <li><b>Constructor Injection (ADR-001):</b> Utilizes an explicit {@link Inject} constructor to guarantee
 *     deterministic bean wiring and AOP proxy reliability.</li>
 * <li><b>Proactive Connection Warm-Up:</b> Establishes database socket connectivity asynchronously via Project Reactor.</li>
 * <li><b>Telemetry & Observability:</b> Logs cluster topology discovery successes and critical connectivity failures.</li>
 * </ul>
 *
 * @author ThinkLab
 * @version 1.0.0
 * @since 1.0
 */
@Singleton
@Slf4j
public class MongoWarmupObserver implements ApplicationEventListener<StartupEvent> {

    private final MongoClient mongoClient;

    /**
     * Explicit constructor for strict dependency injection (ADR-001).
     *
     * @param mongoClient The reactive MongoDB client driver instance. Must not be null.
     * @throws NullPointerException if the mongoClient is null.
     */
    @Inject
    public MongoWarmupObserver(MongoClient mongoClient) {
        this.mongoClient = Objects.requireNonNull(mongoClient, "Application constraint violated: MongoClient cannot be null.");
    }

    /**
     * Intercepts the framework's StartupEvent to proactively initialize the MongoDB connection pool.
     * Constructs a BSON ping command and dispatches it to the admin database via Project Reactor.
     *
     * @param event The application startup event triggered by the IoC container. Must not be null.
     */
    @Override
    public void onApplicationEvent(StartupEvent event) {
        Objects.requireNonNull(event, "Application constraint violated: StartupEvent cannot be null.");

        log.info("[MONGODB_WARMUP] - Initiating proactive SDAM topology discovery...");

        BsonDocument pingCommand = new BsonDocument("ping", new BsonInt32(1));

        Mono.from(mongoClient.getDatabase("admin").runCommand(pingCommand))
                .doOnSuccess(result -> log.info("[MONGODB_WARMUP] - Telemetry established. State: UP. Payload: {}", result))
                .doOnError(error -> log.error("[MONGODB_WARMUP] - CRITICAL: Topology discovery failed. Database unreachable: {}", error.getMessage(), error))
                .subscribe();
    }
}