package com.thinklab.application.port.in;

import com.thinklab.application.usecase.command.ListHashesQuery;
import com.thinklab.domain.model.HashToken;
import jakarta.annotation.Nonnull;
import reactor.core.publisher.Flux;

/**
 * Input Port (UseCase): Defines the contract for listing cryptographic hash registries
 * with pagination and status filtering.
 * Following the CQRS principle, this interface represents a read-only operation
 * designed for high-performance data retrieval and streaming.
 *
 * <p><b>Contractual Obligations:</b></p>
 * <ul>
 *     <li>Implementation must be strictly non-blocking, returning a {@link Flux}.</li>
 *     <li>Must enforce data isolation by strictly adhering to the tenant context
 *         provided in the {@link ListHashesQuery}.</li>
 *     <li>Must correctly apply pagination and sorting metadata to the underlying
 *         persistence layer.</li>
 *     <li>Should support backpressure to ensure system stability during high-volume
 *         data emissions.</li>
 * </ul>
 */
public interface ListHashesUseCase {

    /**
     * Executes the paginated retrieval process for hash tokens based on filter criteria.
     *
     * @param query The immutable query object containing tenant context, optional status,
     *              and pagination parameters.
     * @return A {@link Flux} that emits a stream of {@link HashToken} matching the criteria.
     * @throws IllegalArgumentException if the query is null.
     */
    @Nonnull
    Flux<HashToken> execute(@Nonnull ListHashesQuery query);
}