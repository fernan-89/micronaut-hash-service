package com.thinklab.application.port.out;

import com.thinklab.domain.model.HashAudit;
import jakarta.annotation.Nonnull;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Application Port: Output boundary for the Audit persistence layer.
 * <p>This interface defines the contract for storing and retrieving immutable forensic
 * audit records. Designed for high-assurance environments, this port ensures that
 * security-critical events are persisted effectively without coupling the domain logic
 * to the underlying database implementation.</p>
 *
 * <p><b>Architectural Principles (Mission-Critical Pattern):</b></p>
 * <ul>
 * <li><b>Non-blocking:</b> Fully integrated into the Project Reactor pipeline for high-throughput logging.</li>
 * <li><b>Immutability:</b> Assumes an append-only persistence model for compliance integrity.</li>
 * <li><b>Decoupling:</b> Separates business audit requirements from infrastructure storage concerns.</li>
 * </ul>
 *
 * @version 1.0.0
 */
public interface HashAuditRepositoryPort {

    /**
     * Persists an immutable forensic audit record to the underlying storage.
     *
     * @param audit The {@link HashAudit} instance containing the event details.
     * @return A {@link Mono} emitting the successfully persisted audit entity.
     * @throws NullPointerException if the provided audit object is null.
     */
    @Nonnull
    Mono<HashAudit> save(@Nonnull HashAudit audit);

    /**
     * Retrieves audit records associated with a specific transaction correlation identifier.
     *
     * @param txId The unique transaction identifier.
     * @return A {@link Flux} emitting the list of audit records matching the transaction.
     */
    @Nonnull
    Flux<HashAudit> findByTxId(@Nonnull String txId);

    /**
     * Retrieves all audit records scoped to a specific tenant, facilitating
     * multi-tenant compliance reporting.
     *
     * @param tenantId The unique identifier of the tenant.
     * @return A {@link Flux} emitting all audit records associated with the tenant.
     */
    @Nonnull
    Flux<HashAudit> findByTenantId(@Nonnull String tenantId);

    /**
     * Retrieves the history of forensic audit events for a specific domain entity
     * (e.g., a specific HashToken instance).
     *
     * @param entityId The unique identifier of the target domain entity.
     * @return A {@link Flux} emitting the audit trail for the specified entity.
     */
    @Nonnull
    Flux<HashAudit> findByEntityId(@Nonnull String entityId);
}