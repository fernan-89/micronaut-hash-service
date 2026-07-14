package com.thinklab.application.port.in;

import com.thinklab.application.usecase.command.GetHashQuery;
import com.thinklab.domain.model.HashToken;
import jakarta.annotation.Nonnull;
import reactor.core.publisher.Mono;

/**
 * Application Port: Input boundary for the retrieval of a cryptographic {@link HashToken}.
 * <p>This interface defines the use case contract for querying the registry state.
 * Adhering to CQRS principles, it provides a read-only gateway that decouples
 * external adapters from retrieval logic and underlying storage implementation,
 * ensuring high-performance data access.</p>
 *
 * <p><b>Architectural Principles (Mission-Critical Pattern):</b></p>
 * <ul>
 * <li><b>Non-blocking:</b> Fully integrated into the Project Reactor pipeline for high-throughput retrieval.</li>
 * <li><b>CQRS Compliant:</b> Strictly enforces a separation between read (query) and write (command) operations.</li>
 * <li><b>Data Isolation:</b> Ensures that retrieval operations respect tenant and security boundaries.</li>
 * <li><b>Resilient Error Handling:</b> Provides standardized signaling for missing entities to maintain API contract integrity.</li>
 * </ul>
 *
 * @version 1.0.0
 */
public interface GetHashUseCase {

    /**
     * Orchestrates the secure retrieval of a hash token registry based on specific query criteria.
     *
     * @param query The {@link GetHashQuery} encapsulating the search parameters (e.g., hashId) and security context.
     * @return A {@link Mono} emitting the requested {@link HashToken} if found, or empty signal if not found.
     * @throws NullPointerException if the provided query is null, preserving pipeline integrity (Fail-Fast).
     */
    @Nonnull
    Mono<HashToken> execute(@Nonnull GetHashQuery query);
}