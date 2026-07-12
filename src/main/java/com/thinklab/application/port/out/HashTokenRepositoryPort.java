package com.thinklab.application.port.out;

import com.thinklab.domain.model.HashToken;
import com.thinklab.domain.valueobject.HashStatus;
import io.micronaut.data.model.Pageable;
import jakarta.annotation.Nonnull;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Output Port: Repository contract for the {@link HashToken} aggregate persistence.
 * This interface defines the contract that infrastructure adapters (e.g., MongoDB)
 * must satisfy to provide storage and retrieval capabilities to the Application Layer.
 * It strictly adheres to reactive programming principles to ensure non-blocking execution.
 *
 * <p><b>Architectural Rules:</b></p>
 * <ul>
 *     <li><b>Non-blocking:</b> All methods return Project Reactor types (Mono/Flux).</li>
 *     <li><b>Isolation:</b> Tenant identifiers are mandatory for all multi-tenant queries.</li>
 *     <li><b>Agnostic:</b> Zero coupling with database-specific annotations or frameworks.</li>
 * </ul>
 */
public interface HashTokenRepositoryPort {

    /**
     * Persists or updates a cryptographic hash registry.
     *
     * @param hashToken The aggregate root to be persisted.
     * @return A {@link Mono} emitting the persisted {@link HashToken}.
     */
    @Nonnull
    Mono<HashToken> save(@Nonnull HashToken hashToken);

    /**
     * Retrieves a specific hash registry by its unique system identifier.
     *
     * @param id The unique internal ID of the hash.
     * @return A {@link Mono} emitting the found {@link HashToken}, or empty if not found.
     */
    @Nonnull
    Mono<HashToken> findById(@Nonnull String id);

    /**
     * Checks for the existence of an ACTIVE hash for a specific tenant and payload.
     * Used for duplicate prevention during generation flows.
     *
     * @param tenantId The isolated tenant identifier.
     * @param payload  The original payload to check.
     * @return A {@link Mono} emitting true if an active match exists.
     */
    @Nonnull
    Mono<Boolean> existsActiveByTenantAndPayload(@Nonnull String tenantId, @Nonnull String payload);

    /**
     * Retrieves a paginated stream of hashes belonging to a specific tenant.
     *
     * @param tenantId The isolated tenant identifier.
     * @param pageable Pagination and sorting metadata.
     * @return A {@link Flux} of {@link HashToken} matching the criteria.
     */
    @Nonnull
    Flux<HashToken> findAllByTenantId(@Nonnull String tenantId, @Nonnull Pageable pageable);

    /**
     * Retrieves a paginated stream of hashes filtered by tenant and lifecycle status.
     *
     * @param tenantId The isolated tenant identifier.
     * @param status   The specific status filter.
     * @param pageable Pagination and sorting metadata.
     * @return A {@link Flux} of {@link HashToken} matching the criteria.
     */
    @Nonnull
    Flux<HashToken> findAllByTenantIdAndStatus(@Nonnull String tenantId, @Nonnull HashStatus status, @Nonnull Pageable pageable);
}