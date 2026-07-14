package com.thinklab.infrastructure.adapter.out.mongo.repository;

import com.thinklab.infrastructure.adapter.out.mongo.entity.HashAuditEntity;
import jakarta.annotation.Nonnull;
import io.micronaut.data.mongodb.annotation.MongoRepository;
import io.micronaut.data.repository.reactive.ReactorCrudRepository;
import reactor.core.publisher.Flux;

import java.util.UUID;

/**
 * Infrastructure Adapter: Reactive repository for {@link HashAuditEntity} persistence.
 * <p>This interface defines the infrastructure-level contract for the storage of immutable
 * forensic audit records. It implements the {@link com.thinklab.application.port.out.HashAuditRepositoryPort}
 * using MongoDB, leveraging Micronaut Data's AOT (Ahead-of-Time) compilation to ensure
 * non-blocking, high-performance database interactions.</p>
 *
 * <p><b>Architectural Principles (Mission-Critical Pattern):</b></p>
 * <ul>
 * <li><b>Non-blocking:</b> Inherits from {@link ReactorCrudRepository} for native, high-throughput reactive MongoDB operations.</li>
 * <li><b>Forensic Integrity:</b> Designed for append-only operations; business logic restricts updates or deletions of audit trails.</li>
 * <li><b>AOT Optimized:</b> Query implementations are resolved at compile-time to eliminate reflection overhead.</li>
 * <li><b>Data Segregation:</b> Optimized indices facilitate tenant-scoped queries to ensure compliance and privacy.</li>
 * </ul>
 *
 * @version 1.0.0
 */
@MongoRepository
public interface HashAuditMongoRepository extends ReactorCrudRepository<HashAuditEntity, String> {

    /**
     * Retrieves a reactive stream of audit logs correlated to a specific transaction identifier.
     * Essential for reconstructing execution flows in distributed systems.
     *
     * @param txId The unique transaction identifier.
     * @return A {@link Flux} emitting audit entities for the requested transaction.
     */
    @Nonnull
    Flux<HashAuditEntity> findByTxId(@Nonnull String txId);

    /**
     * Retrieves a reactive stream of audit logs scoped to a specific tenant, ordered chronologically
     * by creation timestamp (newest first).
     *
     * @param tenantId The isolated tenant identifier.
     * @return A {@link Flux} emitting the forensic trail for the tenant.
     */
    @Nonnull
    Flux<HashAuditEntity> findByTenantIdOrderByTimestampDesc(@Nonnull String tenantId);

    /**
     * Retrieves the forensic audit trail mapped to a specific business entity.
     * Leverages native UUID optimization for low-latency lifecycle reconstruction.
     *
     * @param entityId The unique identifier of the target domain entity (UUID).
     * @return A {@link Flux} emitting the matching audit entities.
     */
    @Nonnull
    Flux<HashAuditEntity> findByEntityId(@Nonnull UUID entityId);
}