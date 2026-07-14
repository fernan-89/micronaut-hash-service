package com.thinklab.application.port.out;

import com.thinklab.domain.model.HashToken;
import com.thinklab.domain.valueobject.HashStatus;
import io.micronaut.data.model.Pageable;
import jakarta.annotation.Nonnull;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Application Port: Output boundary for the {@link HashToken} aggregate persistence.
 * <p>This interface defines the contract that infrastructure adapters must satisfy to
 * ensure atomic and consistent storage operations for the cryptographic registry.
 * Designed for high-assurance environments, it strictly enforces reactive, non-blocking
 * communication patterns between the domain and infrastructure layers.</p>
 *
 * <p><b>Architectural Principles (Mission-Critical Pattern):</b></p>
 * <ul>
 * <li><b>Non-blocking:</b> Fully integrated into the Project Reactor pipeline for high-throughput state transitions.</li>
 * <li><b>Consistency:</b> Enforces transactional boundaries for aggregate mutations (updates).</li>
 * <li><b>Isolation:</b> Tenant-scoped operations to ensure data segregation and compliance.</li>
 * <li><b>Integrity:</b> Provides defensive checks to prevent duplicate state injection.</li>
 * </ul>
 *
 * @version 1.0.0
 */
public interface HashTokenRepositoryPort {

    /**
     * Persists the initial state of a new cryptographic hash registry.
     *
     * @param hashToken The aggregate root to be initialized and persisted.
     * @return A {@link Mono} emitting the successfully persisted {@link HashToken}.
     * @throws NullPointerException if the provided hashToken is null.
     */
    @Nonnull
    Mono<HashToken> save(@Nonnull HashToken hashToken);

    /**
     * Commits a state mutation (e.g., transition to REVOKED) of an existing aggregate
     * to the underlying persistent storage.
     *
     * @param hashToken The mutated aggregate root.
     * @return A {@link Mono} emitting the updated {@link HashToken}.
     */
    @Nonnull
    Mono<HashToken> update(@Nonnull HashToken hashToken);

    /**
     * Retrieves a specific hash registry by its unique system identifier.
     *
     * @param id The unique internal identifier of the hash.
     * @return A {@link Mono} emitting the found {@link HashToken}, or empty signal if not found.
     */
    @Nonnull
    Mono<HashToken> findById(@Nonnull String id);

    /**
     * Enforces data integrity by verifying the existence of an ACTIVE hash record
     * for a specific tenant and payload. Used for duplicate prevention in creation flows.
     *
     * @param tenantId The isolated tenant identifier.
     * @param payload  The original payload to validate.
     * @return A {@link Mono} emitting true if an active conflict exists, otherwise false.
     */
    @Nonnull
    Mono<Boolean> existsActiveByTenantAndPayload(@Nonnull String tenantId, @Nonnull String payload);

    /**
     * Retrieves a paginated stream of hashes scoped to a specific tenant.
     *
     * @param tenantId The isolated tenant identifier.
     * @param pageable Pagination and sorting metadata for large dataset handling.
     * @return A {@link Flux} of {@link HashToken} matching the criteria.
     */
    @Nonnull
    Flux<HashToken> findAllByTenantId(@Nonnull String tenantId, @Nonnull Pageable pageable);

    /**
     * Retrieves a paginated stream of hashes filtered by tenant and specific
     * lifecycle status.
     *
     * @param tenantId The isolated tenant identifier.
     * @param status   The specific {@link HashStatus} filter (e.g., ACTIVE, REVOKED).
     * @param pageable Pagination and sorting metadata.
     * @return A {@link Flux} of {@link HashToken} matching the criteria.
     */
    @Nonnull
    Flux<HashToken> findAllByTenantIdAndStatus(@Nonnull String tenantId, @Nonnull HashStatus status, @Nonnull Pageable pageable);
}