package com.thinklab.infrastructure.adapter.out.mongo;

import com.thinklab.application.port.out.HashAuditRepositoryPort;
import com.thinklab.domain.model.HashAudit;
import com.thinklab.infrastructure.adapter.out.mongo.entity.HashAuditEntity;
import com.thinklab.infrastructure.adapter.out.mongo.repository.HashAuditMongoRepository;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Objects;
import java.util.UUID;

/**
 * Infrastructure Adapter: Implementation of the {@link HashAuditRepositoryPort} for MongoDB.
 *
 * <p><b>Architectural Role:</b>
 * This adapter serves as the Anti-Corruption Layer (ACL) between the core Domain layer and the
 * MongoDB persistence engine. It handles bidirectional translation between domain aggregates and
 * infrastructure entities, ensuring complete isolation of database concerns from business logic.
 *
 * <p><b>Contractual Obligations:</b>
 * <ul>
 * <li><b>Constructor Injection (ADR-001):</b> Uses an explicit {@link Inject} constructor instead of
 *     Lombok generation to guarantee deterministic bean wiring and AOP proxy reliability.</li>
 * <li><b>Non-Blocking I/O:</b> 100% reactive pipeline using Project Reactor {@link Mono} and {@link Flux},
 *     prohibiting blocking calls and protecting Netty EventLoops.</li>
 * <li><b>Identity Sovereignty:</b> Enforces native {@link UUID} parameters to maintain strict
 *     BSON Binary (Subtype 4) performance optimization.</li>
 * <li><b>Append-Only Integrity:</b> Enforces immutable writing of audit events for security compliance.</li>
 * </ul>
 *
 * @author ThinkLab
 * @version 1.0.0
 * @since 1.0
 */
@Slf4j
@Singleton
public class HashAuditRepositoryAdapter implements HashAuditRepositoryPort {

    private final HashAuditMongoRepository repository;

    /**
     * Explicit constructor for strict dependency injection (ADR-001).
     *
     * @param repository The underlying MongoDB reactive repository for audit persistence. Must not be null.
     * @throws NullPointerException if the repository instance is null.
     */
    @Inject
    public HashAuditRepositoryAdapter(HashAuditMongoRepository repository) {
        this.repository = Objects.requireNonNull(repository, "Infrastructure constraint violated: HashAuditMongoRepository cannot be null.");
    }

    /**
     * Persists an immutable forensic audit record to MongoDB in a non-blocking manner.
     *
     * @param audit The immutable {@link HashAudit} domain record to persist. Must not be null.
     * @return A {@link Mono} emitting the successfully persisted domain audit record.
     * @throws NullPointerException if the provided audit record is null.
     */
    @Override
    public Mono<HashAudit> save(HashAudit audit) {
        Objects.requireNonNull(audit, "Infrastructure constraint violated: Audit record cannot be null for persistence.");

        return Mono.just(audit)
                .map(HashAuditEntity::fromDomain)
                .flatMap(repository::save)
                .map(HashAuditEntity::toDomain)
                .doOnSubscribe(s -> log.debug("[ACTION: PERSIST_AUDIT_LOG] [TX: {}] - Initiating write operation for audit entity: {}", audit.txId(), audit.id()))
                .doOnSuccess(saved -> log.debug("[ACTION: PERSIST_AUDIT_LOG] [TX: {}] - Forensic audit event successfully committed.", saved.txId()))
                .doOnError(e -> log.error("[ACTION: PERSIST_AUDIT_LOG] [TX: {}] - CRITICAL: Failed to commit audit log. Error: {}", audit.txId(), e.getMessage(), e));
    }

    /**
     * Retrieves the reactive audit trail correlated to a specific transaction UUID.
     *
     * @param txId The transaction correlation UUID. Must not be null.
     * @return A {@link Flux} emitting matching domain audit records.
     * @throws NullPointerException if the provided transaction UUID is null.
     */
    @Override
    public Flux<HashAudit> findByTxId(UUID txId) {
        Objects.requireNonNull(txId, "Infrastructure constraint violated: Transaction UUID is mandatory for retrieval.");

        return repository.findByTxId(txId)
                .map(HashAuditEntity::toDomain)
                .doOnError(e -> log.error("[ACTION: FIND_AUDIT_TX] [TX: {}] - Error retrieving logs: {}", txId, e.getMessage(), e));
    }

    /**
     * Retrieves audit logs scoped strictly to an isolated tenant boundary, ordered by newest first.
     *
     * @param tenantId The isolated tenant identifier. Must not be null or blank.
     * @return A {@link Flux} emitting the chronological audit records for the tenant.
     * @throws NullPointerException if the tenant ID is null.
     * @throws IllegalArgumentException if the tenant ID is blank.
     */
    @Override
    public Flux<HashAudit> findByTenantId(String tenantId) {
        Objects.requireNonNull(tenantId, "Infrastructure constraint violated: Tenant identifier is mandatory.");
        if (tenantId.isBlank()) {
            throw new IllegalArgumentException("Infrastructure constraint violated: Tenant identifier cannot be blank.");
        }

        return repository.findByTenantIdOrderByTimestampDesc(tenantId)
                .map(HashAuditEntity::toDomain)
                .doOnError(e -> log.error("[ACTION: FIND_AUDIT_TENANT] [TENANT: {}] - Error retrieving tenant logs: {}", tenantId, e.getMessage(), e));
    }

    /**
     * Retrieves the forensic audit trail mapped to a specific business entity UUID.
     *
     * @param entityId The universal unique identifier (UUID) of the target domain entity. Must not be null.
     * @return A {@link Flux} emitting matching domain audit records.
     * @throws NullPointerException if the provided entity UUID is null.
     */
    @Override
    public Flux<HashAudit> findByEntityId(UUID entityId) {
        Objects.requireNonNull(entityId, "Infrastructure constraint violated: Entity UUID is mandatory for audit retrieval.");

        return repository.findByEntityId(entityId)
                .map(HashAuditEntity::toDomain)
                .doOnError(e -> log.error("[ACTION: FIND_AUDIT_ENTITY] [ID: {}] - Error retrieving historical logs: {}", entityId, e.getMessage(), e));
    }
}