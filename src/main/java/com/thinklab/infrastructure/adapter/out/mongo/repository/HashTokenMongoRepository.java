package com.thinklab.infrastructure.adapter.out.mongo.repository;

import com.thinklab.domain.valueobject.HashStatus;
import com.thinklab.infrastructure.adapter.out.mongo.entity.HashTokenEntity;
import jakarta.annotation.Nonnull;
import io.micronaut.data.model.Pageable;
import io.micronaut.data.mongodb.annotation.MongoRepository;
import io.micronaut.data.repository.reactive.ReactorCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Infrastructure Adapter: Reactive repository for {@link HashTokenEntity} persistence.
 * <p>This interface implements the {@link com.thinklab.application.port.out.HashTokenRepositoryPort}
 * contract, leveraging Micronaut Data's AOT (Ahead-of-Time) compilation for efficient,
 * non-blocking MongoDB interactions. It provides type-safe query derivation for the Hash Registry.</p>
 *
 * <p><b>Architectural Principles (Mission-Critical Pattern):</b></p>
 * <ul>
 * <li><b>Non-blocking:</b> Inherits from {@link ReactorCrudRepository} for native, high-throughput reactive MongoDB operations.</li>
 * <li><b>AOT Optimized:</b> Query implementations are resolved at compile-time to eliminate runtime reflection overhead.</li>
 * <li><b>Performance-Oriented:</b> Uses {@link UUID} identifiers to leverage BSON Binary Subtype 4 for reduced index size.</li>
 * <li><b>Data Segregation:</b> Enforces strict tenant scoping to ensure data isolation and compliance.</li>
 * </ul>
 *
 * <p><b>🚀 STAFF ENGINEER NOTE (Nível NASA):</b><br>
 * O tipo do ID foi definido como {@link UUID} para refletir o armazenamento nativo
 * otimizado (BSON Binary Subtype 4) implementado na {@link HashTokenEntity}.
 * Isso reduz o overhead de armazenamento e acelera as operações de junção/busca em larga escala.</p>
 *
 * @version 1.0.0
 */
@MongoRepository
public interface HashTokenMongoRepository extends ReactorCrudRepository<HashTokenEntity, UUID> {

    /**
     * Verifies the existence of an active hash for the given tenant and raw payload.
     * Used for business-level uniqueness validation during generation flows to prevent duplicates.
     *
     * @param tenantId The isolated tenant identifier.
     * @param payload  The original payload string.
     * @param status   The target operational status filter (e.g., ACTIVE).
     * @return A {@link Mono} emitting true if a match is found, false otherwise.
     */
    @Nonnull
    Mono<Boolean> existsByTenantIdAndPayloadAndStatus(
            @Nonnull String tenantId,
            @Nonnull String payload,
            @Nonnull HashStatus status);

    /**
     * Verifies the existence of an active hash for the given tenant and generated hash value.
     * Used for business-level collision prevention.
     *
     * @param tenantId      The isolated tenant identifier.
     * @param generatedHash The calculated cryptographic hash string.
     * @param status        The target operational status filter (e.g., ACTIVE).
     * @return A {@link Mono} emitting true if a match is found, false otherwise.
     */
    @Nonnull
    Mono<Boolean> existsByTenantIdAndGeneratedHashAndStatus(
            @Nonnull String tenantId,
            @Nonnull String generatedHash,
            @Nonnull HashStatus status);

    /**
     * Retrieves a paginated stream of hash entities filtered by tenant and status.
     * Essential for high-performance compliance reporting and dashboard filtering.
     *
     * @param tenantId The isolated tenant identifier.
     * @param status   The target operational status.
     * @param pageable The pagination and sorting metadata.
     * @return A {@link Flux} emitting matching hash entities.
     */
    @Nonnull
    Flux<HashTokenEntity> findByTenantIdAndStatus(
            @Nonnull String tenantId,
            @Nonnull HashStatus status,
            @Nonnull Pageable pageable);

    /**
     * Retrieves a paginated stream of all hash entities associated with a specific tenant.
     *
     * @param tenantId The isolated tenant identifier.
     * @param pageable The pagination and sorting metadata.
     * @return A {@link Flux} emitting the tenant's hash entities.
     */
    @Nonnull
    Flux<HashTokenEntity> findByTenantId(@Nonnull String tenantId, @Nonnull Pageable pageable);
}