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

/**
 * Persistence Adapter: Implementation of the {@link HashTokenRepositoryPort} for MongoDB.
 * This class acts as a bridge between the Application Layer and the physical database,
 * encapsulating the mapping logic to shield the Domain from persistence-specific details.
 *
 * <p><b>Architectural Role (NASA Standards):</b></p>
 * <ul>
 *     <li><b>Anti-Corruption Layer (ACL):</b> Bidirectional mapping between Domain Aggregates
 *         and Infrastructure Entities.</li>
 *     <li><b>Reactive Ready:</b> Non-blocking execution optimized for Project Reactor streams.</li>
 *     <li><b>Data Integrity:</b> Leverages the repository's support for versioning and indexing.</li>
 * </ul>
 */
@Slf4j
@Singleton
@RequiredArgsConstructor
public class HashTokenRepositoryAdapter implements HashTokenRepositoryPort {

    private final HashTokenMongoRepository repository;

    /**
     * Persists a hash aggregate into the MongoDB collection.
     *
     * @param hashToken The pure domain aggregate to be saved.
     * @return A {@link Mono} emitting the persisted domain aggregate.
     */
    @Override
    @Nonnull
    public Mono<HashToken> save(@Nonnull HashToken hashToken) {
        Objects.requireNonNull(hashToken, "HashToken aggregate is mandatory for persistence");
        log.debug("Persistence Adapter: Saving hash token for tenant [{}]", hashToken.tenantId());

        return repository.save(HashTokenEntity.fromDomain(hashToken))
                .map(HashTokenEntity::toDomain)
                .doOnError(e -> log.error("Persistence Error: Failed to save hash token for tenant [{}]",
                        hashToken.tenantId(), e));
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
        log.trace("Persistence Adapter: Fetching hash registry by ID [{}]", id);

        return repository.findById(id)
                .map(HashTokenEntity::toDomain);
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
        log.trace("Persistence Adapter: Checking active hash existence in tenant [{}]", tenantId);
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
        log.trace("Persistence Adapter: Listing tenant [{}] hashes", tenantId);
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
        log.trace("Persistence Adapter: Listing tenant [{}] hashes with status [{}]", tenantId, status);
        return repository.findByTenantIdAndStatus(tenantId, status, pageable)
                .map(HashTokenEntity::toDomain);
    }
}