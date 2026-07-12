package com.thinklab.infrastructure.adapter.out.mongo;

import com.thinklab.application.port.out.HashAuditRepositoryPort;
import com.thinklab.domain.model.HashAudit;
import com.thinklab.infrastructure.adapter.out.mongo.entity.HashAuditEntity;
import com.thinklab.infrastructure.adapter.out.mongo.repository.HashAuditMongoRepository;
import jakarta.annotation.Nonnull;
import jakarta.inject.Singleton;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Objects;
import java.util.UUID;

/**
 * Persistence Adapter: Implementation of the {@link HashAuditRepositoryPort} for MongoDB.
 * This class acts as a bridge between the Application Layer and the physical storage,
 * providing a sanitized interface for forensic audit trail persistence.
 *
 * <p><b>Architectural Roles:</b></p>
 * <ul>
 *     <li><b>Anti-Corruption Layer (ACL):</b> Encapsulates the translation between Domain
 *         models and Infrastructure entities.</li>
 *     <li><b>Reactive Protocol:</b> Leverages non-blocking I/O to ensure system responsiveness
 *         during high-volume audit writing.</li>
 *     <li><b>Append-Only Enforcement:</b> While inheriting standard operations, this adapter
 *         focuses on the immutability of recorded events.</li>
 * </ul>
 */
@Slf4j
@Singleton
@RequiredArgsConstructor
public class HashAuditRepositoryAdapter implements HashAuditRepositoryPort {

    private final HashAuditMongoRepository repository;

    /**
     * Records a new forensic audit entry into the MongoDB "hash_audit" collection.
     *
     * @param audit The pure domain audit record to be persisted.
     * @return A {@link Mono} emitting the saved domain audit instance.
     */
    @Override
    @Nonnull
    public Mono<HashAudit> save(@Nonnull HashAudit audit) {
        Objects.requireNonNull(audit, "Audit record cannot be null for persistence");

        log.debug("Persistence Adapter: Recording audit event for TX [{}] - Operation [{}]",
                audit.txId(), audit.operation());

        return repository.save(HashAuditEntity.fromDomain(audit))
                .map(HashAuditEntity::toDomain)
                .doOnError(e -> log.error("Persistence Error: Failed to commit audit log for TX [{}]",
                        audit.txId(), e));
    }

    /**
     * Retrieves a stream of audit logs associated with a specific transaction.
     *
     * @param txId The unique transaction group identifier.
     * @return A {@link Flux} of matching domain audit records.
     */
    @Override
    @Nonnull
    public Flux<HashAudit> findByTxId(@Nonnull String txId) {
        Objects.requireNonNull(txId, "Transaction ID is mandatory for retrieval");

        log.trace("Persistence Adapter: Fetching audit trail for TX [{}]", txId);

        return repository.findByTxId(txId)
                .map(HashAuditEntity::toDomain);
    }

    /**
     * Retrieves all audit logs belonging to a specific tenant.
     *
     * @param tenantId The isolated tenant identifier.
     * @return A {@link Flux} of domain audit records ordered by newest first.
     */
    @Override
    @Nonnull
    public Flux<HashAudit> findByTenantId(@Nonnull String tenantId) {
        Objects.requireNonNull(tenantId, "Tenant identifier is mandatory for audit listing");

        return repository.findByTenantIdOrderByTimestampDesc(tenantId)
                .map(HashAuditEntity::toDomain);
    }

    /**
     * Retrieves all audit logs matching a specific entity identifier.
     *
     * @param entityId The targeted business entity identifier.
     * @return A {@link Flux} of matching domain audit records.
     */
    @Override
    @Nonnull
    public Flux<HashAudit> findByEntityId(@Nonnull String entityId) {
        Objects.requireNonNull(entityId, "Entity identifier is mandatory for audit listing");

        log.trace("Persistence Adapter: Fetching audit trail for Entity [{}]", entityId);

        // 🚀 STAFF ENGINEER NOTE: Realiza o parse seguro de String para UUID na camada de acl/infraestrutura
        return repository.findByEntityId(UUID.fromString(entityId))
                .map(HashAuditEntity::toDomain);
    }
}