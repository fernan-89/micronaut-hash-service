package com.thinklab.infrastructure.adapter.out.mongo.repository;

import com.thinklab.infrastructure.adapter.out.mongo.entity.HashAuditEntity;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.data.mongodb.annotation.MongoRepository;
import io.micronaut.data.repository.reactive.ReactorCrudRepository;
import reactor.core.publisher.Flux;

import java.util.UUID;

/**
 * Micronaut Data Repository: Reactive persistence interface for {@link HashAuditEntity}.
 * This repository handles the append-only storage of forensic audit records. It utilizes
 * Micronaut Data's AOT (Ahead-of-Time) compilation to provide highly performant,
 * non-blocking access to the MongoDB "hash_audit" collection.
 *
 * <p><b>Persistence Strategy (NASA Standards):</b></p>
 * <ul>
 *     <li><b>Reactive Protocol:</b> Inherits from {@link ReactorCrudRepository} for
 *         native specialized MongoDB reactive support, ensuring proper bean injection.</li>
 *     <li><b>Forensic Integrity:</b> Designed for immutable trails; while standard CRUD
 *         is inherited, business rules prohibit updates or deletions of audit logs.</li>
 *     <li><b>AOT Optimized:</b> Query implementations are generated at compile-time,
 *         eliminating runtime reflection overhead and reducing memory footprint.</li>
 * </ul>
 */
@MongoRepository
public interface HashAuditMongoRepository extends ReactorCrudRepository<HashAuditEntity, String> {

    /**
     * Retrieves a stream of audit logs associated with a specific transaction group.
     * Essential for tracing correlated operations across the distributed system.
     *
     * @param txId The unique transaction identifier used for correlation.
     * @return A {@link Flux} emitting audit entities for the requested transaction.
     */
    @NonNull
    Flux<HashAuditEntity> findByTxId(@NonNull String txId);

    /**
     * Retrieves all audit logs belonging to a specific tenant, sorted by execution time.
     * Optimized for security dashboards and compliance reports.
     *
     * @param tenantId The isolated tenant identifier.
     * @return A {@link Flux} emitting the forensic trail for the tenant, ordered by newest first.
     */
    @NonNull
    Flux<HashAuditEntity> findByTenantIdOrderByTimestampDesc(@NonNull String tenantId);

    /**
     * Retrieves all forensic audit logs mapped to a specific business entity.
     * Leverages the native UUID optimization to trace lifecycle modifications of target domain models.
     *
     * @param entityId The targeted business entity unique identifier (UUID).
     * @return A {@link Flux} emitting matching audit entities for the requested business entity.
     */
    @NonNull
    Flux<HashAuditEntity> findByEntityId(@NonNull UUID entityId);
}