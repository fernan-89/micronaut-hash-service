package com.thinklab.infrastructure.adapter.out.mongo;

import com.thinklab.application.port.out.HashTokenRepositoryPort;
import com.thinklab.domain.model.HashToken;
import com.thinklab.domain.valueobject.HashStatus;
import com.thinklab.infrastructure.adapter.out.mongo.entity.HashTokenEntity;
import com.thinklab.infrastructure.adapter.out.mongo.repository.HashTokenMongoRepository;
import io.micronaut.data.model.Pageable;
import jakarta.annotation.Nonnull;
import jakarta.inject.Singleton;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Objects;
import java.util.UUID;

/**
 * Infrastructure Adapter: Implementation of the {@link HashTokenRepositoryPort} for MongoDB.
 * <p>This adapter serves as the Anti-Corruption Layer (ACL) between the Domain layer and
 * the infrastructure storage. It encapsulates the mapping logic to shield the domain
 * from persistence-specific details, ensuring robust data lifecycle management.</p>
 *
 * <p><b>Architectural Principles (Mission-Critical Pattern):</b></p>
 * <ul>
 * <li><b>Anti-Corruption Layer (ACL):</b> Bidirectional translation between Domain Aggregates and Infrastructure Entities.</li>
 * <li><b>Non-blocking:</b> Fully integrated into the Project Reactor pipeline for high-throughput I/O.</li>
 * <li><b>Data Integrity:</b> Enforces schema versioning and transactional consistency via optimistic locking.</li>
 * </ul>
 *
 * @version 1.0.0
 */
@Slf4j
@Singleton
@RequiredArgsConstructor
public class HashTokenRepositoryAdapter implements HashTokenRepositoryPort {

    private final HashTokenMongoRepository repository;

    /**
     * Persists a newly created hash aggregate into the MongoDB collection.
     *
     * @param hashToken The pure domain aggregate to be saved.
     * @return A {@link Mono} emitting the persisted domain aggregate.
     */
    @Override
    @Nonnull
    public Mono<HashToken> save(@Nonnull HashToken hashToken) {
        Objects.requireNonNull(hashToken, "HashToken aggregate is mandatory for persistence");
        log.debug("[ACTION: PERSIST_TOKEN] - Saving new hash token for tenant: {}", hashToken.tenantId());

        return repository.save(HashTokenEntity.fromDomain(hashToken))
                .map(HashTokenEntity::toDomain)
                .doOnError(e -> log.error("[ACTION: PERSIST_TOKEN] - CRITICAL: Failed to save hash token for tenant: {}. Error: {}", hashToken.tenantId(), e.getMessage()));
    }

    /**
     * Updates an existing hash aggregate in the MongoDB collection.
     *
     * @param hashToken The modified domain aggregate to be updated.
     * @return A {@link Mono} emitting the updated domain aggregate.
     */
    @Nonnull
    @Override
    public Mono<HashToken> update(@Nonnull HashToken hashToken) {
        Objects.requireNonNull(hashToken, "HashToken aggregate is mandatory for update");
        log.debug("[ACTION: UPDATE_TOKEN] - Updating hash token for tenant: {}", hashToken.tenantId());

        return repository.update(HashTokenEntity.fromDomain(hashToken))
                .map(HashTokenEntity::toDomain)
                .doOnError(e -> log.error("[ACTION: UPDATE_TOKEN] - CRITICAL: Failed to update hash token for tenant: {}. Error: {}", hashToken.tenantId(), e.getMessage()));
    }

    /**
     * Retrieves a hash token by its primary identifier.
     *
     * @param id The unique system identifier.
     * @return A {@link Mono} emitting the found hash token or empty if not present.
     */
    @Override
    @Nonnull
    public Mono<HashToken> findById(@Nonnull String id) {
        Objects.requireNonNull(id, "Identifier is mandatory for retrieval");
        log.trace("[ACTION: FIND_TOKEN_ID] - Fetching hash registry by ID: {}", id);

        try {
            UUID nativeId = UUID.fromString(id);
            return repository.findById(nativeId)
                    .map(HashTokenEntity::toDomain);
        } catch (IllegalArgumentException e) {
            log.warn("[ACTION: FIND_TOKEN_ID] - Invalid UUID format provided: {}", id);
            return Mono.empty();
        }
    }

    /**
     * Checks for the existence of an ACTIVE hash for a specific tenant and raw payload.
     *
     * @param tenantId The isolated tenant context.
     * @param payload  The original payload string.
     * @return A {@link Mono} emitting true if an active registry already exists.
     */
    @Override
    @Nonnull
    public Mono<Boolean> existsActiveByTenantAndPayload(@Nonnull String tenantId, @Nonnull String payload) {
        log.trace("[ACTION: EXISTS_TOKEN] - Checking active hash existence in tenant: {}", tenantId);
        return repository.existsByTenantIdAndPayloadAndStatus(tenantId, payload, HashStatus.ACTIVE);
    }

    /**
     * Retrieves all hashes for a tenant using reactive pagination.
     *
     * @param tenantId The isolated tenant context.
     * @param pageable The pagination and sorting metadata.
     * @return A {@link Flux} stream of matching domain aggregates.
     */
    @Override
    @Nonnull
    public Flux<HashToken> findAllByTenantId(@Nonnull String tenantId, @Nonnull Pageable pageable) {
        log.trace("[ACTION: LIST_TOKENS] - Listing tenant: {} hashes", tenantId);
        return repository.findByTenantId(tenantId, pageable)
                .map(HashTokenEntity::toDomain);
    }

    /**
     * Retrieves filtered hashes for a tenant using reactive pagination.
     *
     * @param tenantId The isolated tenant context.
     * @param status   The specific operational status to filter.
     * @param pageable The pagination and sorting metadata.
     * @return A {@link Flux} stream of matching domain aggregates.
     */
    @Override
    @Nonnull
    public Flux<HashToken> findAllByTenantIdAndStatus(@Nonnull String tenantId, @Nonnull HashStatus status, @Nonnull Pageable pageable) {
        log.trace("[ACTION: LIST_TOKENS_STATUS] - Listing tenant: {} hashes with status: {}", tenantId, status);
        return repository.findByTenantIdAndStatus(tenantId, status, pageable)
                .map(HashTokenEntity::toDomain);
    }
}