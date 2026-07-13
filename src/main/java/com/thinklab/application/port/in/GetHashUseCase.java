package com.thinklab.application.port.in;

import com.thinklab.application.usecase.command.GetHashQuery;
import com.thinklab.domain.model.HashToken;
import jakarta.annotation.Nonnull;
import reactor.core.publisher.Mono;

/**
 * Input Port (UseCase): Defines the contract for retrieving a specific cryptographic hash token.
 * Following the CQRS (Command Query Responsibility Segregation) principle, this interface
 * represents a pure read-only operation. It ensures that inbound adapters remain
 * decoupled from the retrieval logic and database specifics.
 *
 * <p><b>Contractual Obligations:</b></p>
 * <ul>
 *     <li>Implementation must be strictly non-blocking, returning a {@link Mono}.</li>
 *     <li>Must signal {@link com.thinklab.domain.exception.HashNotFoundException} if the requested
 *         identifier does not exist in the persistence layer.</li>
 *     <li>Input parameters must be validated via {@link GetHashQuery} before execution.</li>
 * </ul>
 */
public interface GetHashUseCase {

    /**
     * Retrieves a hash token registry based on the provided query criteria.
     *
     * @param query The immutable query containing the search parameters (e.g., hashId).
     * @return A {@link Mono} that emits the found {@link HashToken}.
     * @throws IllegalArgumentException if the query is null.
     */
    @Nonnull
    Mono<HashToken> execute(@Nonnull GetHashQuery query);
}