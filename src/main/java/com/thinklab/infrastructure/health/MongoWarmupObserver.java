package com.thinklab.infrastructure.health;

import com.mongodb.reactivestreams.client.MongoClient;
import io.micronaut.context.event.ApplicationEventListener;
import io.micronaut.context.event.StartupEvent;
import jakarta.inject.Singleton;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.BsonDocument;
import org.bson.BsonInt32;
import reactor.core.publisher.Mono;

// ==============================================================================================
// /**
//  * @class       MongoWarmupObserver
//  * @module      Thinklab Infrastructure / Telemetry
//  * @description Proactively initializes the MongoDB Reactive Driver connection pool.
//  * By executing a deterministic 'ping' command upon application startup,
//  * this observer forces the Server Discovery and Monitoring (SDAM) mechanism
//  * to resolve the cluster topology before the Kubernetes Readiness Probe
//  * polls the /health endpoint. This eliminates 'UNKNOWN' states and cold-start latency.
//  *
//  * @pattern     Eager Initialization / Connection Warm-up
//  * @maintainer  Thinklab Systems Engineering Team
//  */
// ==============================================================================================
@Singleton
@Slf4j
@RequiredArgsConstructor
public class MongoWarmupObserver implements ApplicationEventListener<StartupEvent> {

    private final MongoClient mongoClient;

    // /**
    //  * @directive   onApplicationEvent
    //  * @description Intercepts the framework's StartupEvent. Constructs a BSON ping command
    //  * and dispatches it to the admin database via Project Reactor (Mono).
    //  */
    @Override
    public void onApplicationEvent(StartupEvent event) {
        log.info("[MONGODB_WARMUP] - Initiating proactive SDAM topology discovery...");

        BsonDocument pingCommand = new BsonDocument("ping", new BsonInt32(1));

        Mono.from(mongoClient.getDatabase("admin").runCommand(pingCommand))
                .doOnSuccess(result -> log.info("[MONGODB_WARMUP] - Telemetry established. State: UP. Payload: {}", result))
                .doOnError(error -> log.error("[MONGODB_WARMUP] - CRITICAL: Topology discovery failed. Database unreachable.", error))
                .subscribe();
    }
}