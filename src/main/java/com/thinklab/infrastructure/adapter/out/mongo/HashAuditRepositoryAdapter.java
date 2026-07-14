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
 * Infrastructure Adapter: Implementation of the {@link HashAuditRepositoryPort} for MongoDB.
 * <p>This adapter serves as the Anti-Corruption Layer (ACL) between the Domain layer and
 * the infrastructure storage. It ensures that infrastructure-specific concerns (like entity
 * mapping) remain decoupled from domain logic, while enforcing strict audit integrity.</p>
 *
 * <p><b>Architectural Principles (Mission-Critical Pattern):</b></p>
 * <ul>
 * <li><b>Anti-Corruption Layer (ACL):</b> Encapsulates the bidirectional translation between domain models and persistence entities.</li>
 * <li><b>Non-blocking:</b> Leverages reactive streams to ensure minimal latency in logging critical events.</li>
 * <li><b>Forensic Integrity:</b> Designed for append-only storage to maintain the immutable nature of audit trails.</li>
 * <li><b>Operational Transparency:</b> Standardized logging ensures that persistence failures are traceable for forensic analysis.</li>
 * </ul>
 *
 * @version 1.0.0
 */
@Slf4j
@Singleton
@RequiredArgsConstructor
public class HashAuditRepositoryAdapter implements HashAuditRepositoryPort {

    private final HashAuditMongoRepository repository;

    /**
     * Persists an immutable forensic audit record.
     *
     * @param audit The domain audit record.
     * @return A {@link Mono} emitting the persisted record.
     * @throws NullPointerException if the audit object is null.
     */
    @Override
    @Nonnull
    public Mono<HashAudit> save(@Nonnull HashAudit audit) {
        Objects.requireNonNull(audit, "Audit record cannot be null for persistence");

        return repository.save(HashAuditEntity.fromDomain(audit))
                .map(HashAuditEntity::toDomain)
                .doOnSubscribe(s -> log.debug("[ACTION: PERSIST_AUDIT_LOG] - Initiating write operation for TX: {}", audit.txId()))
                .doOnSuccess(saved -> log.debug("[ACTION: PERSIST_AUDIT_LOG] - Forensic audit event successfully committed. TX: {}", saved.txId()))
                .doOnError(e -> log.error("[ACTION: PERSIST_AUDIT_LOG] - CRITICAL: Failed to commit audit log for TX: {}. Error: {}", audit.txId(), e.getMessage()));
    }

    /**
     * Retrieves the audit trail correlated to a specific transaction.
     *
     * @param txId The transaction correlation identifier.
     * @return A {@link Flux} of matching audit records.
     */
    @Override
    @Nonnull
    public Flux<HashAudit> findByTxId(@Nonnull String txId) {
        Objects.requireNonNull(txId, "Transaction ID is mandatory for retrieval");

        return repository.findByTxId(txId)
                .map(HashAuditEntity::toDomain)
                .doOnError(e -> log.error("[ACTION: FIND_AUDIT_TX] - Error retrieving logs for TX: {}. Error: {}", txId, e.getMessage()));
    }

    /**
     * Retrieves audit logs scoped to a specific tenant.
     *
     * @param tenantId The isolated tenant identifier.
     * @return A {@link Flux} of audit records.
     */
    @Override
    @Nonnull
    public Flux<HashAudit> findByTenantId(@Nonnull String tenantId) {
        Objects.requireNonNull(tenantId, "Tenant identifier is mandatory");

        return repository.findByTenantIdOrderByTimestampDesc(tenantId)
                .map(HashAuditEntity::toDomain);
    }

    /**
     * Retrieves the forensic audit trail for a specific domain entity.
     *
     * @param entityId The targeted business entity identifier.
     * @return A {@link Flux} of matching audit records.
     */
    @Override
    @Nonnull
    public Flux<HashAudit> findByEntityId(@Nonnull String entityId) {
        Objects.requireNonNull(entityId, "Entity identifier is mandatory");

        return repository.findByEntityId(UUID.fromString(entityId))
                .map(HashAuditEntity::toDomain)
                .doOnError(e -> log.error("[ACTION: FIND_AUDIT_ENTITY] - Error retrieving logs for Entity: {}. Error: {}", entityId, e.getMessage()));
    }
}