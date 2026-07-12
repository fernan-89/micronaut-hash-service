package com.thinklab.infrastructure.adapter.out.mongo.repository;

import com.thinklab.domain.valueobject.HashStatus;
import com.thinklab.infrastructure.adapter.out.mongo.entity.HashTokenEntity;
import jakarta.annotation.Nonnull;
import io.micronaut.data.model.Pageable;
import io.micronaut.data.mongodb.annotation.MongoRepository;
import io.micronaut.data.repository.reactive.ReactorCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Micronaut Data Repository: Reactive persistence interface for {@link HashTokenEntity}.
 * This interface leverages Micronaut Data's AOT compilation to generate efficient,
 * non-blocking MongoDB driver calls. It handles all CRUD operations reactively
 * and provides type-safe query derivation for the Hash Registry.
 *
 * <p><b>Persistence Strategy:</b></p>
 * <ul>
 *     <li><b>Reactive Mongo:</b> Extends {@link ReactiveMongoRepository} for specialized
 *         MongoDB reactive support.</li>
 *     <li><b>Zero Reflection:</b> Implementations are generated at compile-time to
 *         optimize startup and footprint.</li>
 *     <li><b>Data Integrity:</b> Supports optimistic locking via the entity's version field.</li>
 * </ul>
 */
@MongoRepository
public interface HashTokenMongoRepository extends ReactorCrudRepository<HashTokenEntity, String> {

    /**
     * Checks if an active hash already exists for the given tenant and raw payload.
     * Used for business-level uniqueness validation during generation.
     *
     * @param tenantId The isolated tenant identifier.
     * @param payload  The original payload string.
     * @param status   The target operational status (e.g., ACTIVE).
     * @return A {@link Mono} emitting true if a match is found.
     */
    @Nonnull
    Mono<Boolean> existsByTenantIdAndPayloadAndStatus(
            @Nonnull String tenantId,
            @Nonnull String payload,
            @Nonnull HashStatus status);

    /**
     * Checks if an active hash already exists for the given tenant and payload hash.
     * Used for business-level uniqueness validation during generation.
     *
     * @param tenantId      The isolated tenant identifier.
     * @param generatedHash The calculated cryptographic hash string.
     * @param status        The target operational status (e.g., ACTIVE).
     * @return A {@link Mono} emitting true if a match is found.
     */
    @Nonnull
    Mono<Boolean> existsByTenantIdAndGeneratedHashAndStatus(
            @Nonnull String tenantId,
            @Nonnull String generatedHash,
            @Nonnull HashStatus status);

    /**
     * Retrieves a paginated stream of hashes filtered by tenant and status.
     *
     * @param tenantId The isolated tenant identifier.
     * @param status   The current operational status.
     * @param pageable The pagination and sorting metadata.
     * @return A {@link Flux} emitting matching hash entities.
     */
    @Nonnull
    Flux<HashTokenEntity> findByTenantIdAndStatus(
            @Nonnull String tenantId,
            @Nonnull HashStatus status,
            @Nonnull Pageable pageable);

    /**
     * Retrieves all hashes belonging to a specific tenant with pagination.
     *
     * @param tenantId The isolated tenant identifier.
     * @param pageable The pagination and sorting metadata.
     * @return A {@link Flux} emitting the tenant's hash entities.
     */
    @Nonnull
    Flux<HashTokenEntity> findByTenantId(@Nonnull String tenantId, @Nonnull Pageable pageable);
}