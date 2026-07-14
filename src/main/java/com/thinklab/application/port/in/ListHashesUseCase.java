package com.thinklab.application.port.in;

import com.thinklab.application.usecase.command.ListHashesQuery;
import com.thinklab.domain.model.HashToken;
import jakarta.annotation.Nonnull;
import reactor.core.publisher.Flux;

/**
 * Application Port: Input boundary for the paginated retrieval of {@link HashToken} registries.
 * <p>This interface defines the use case contract for querying registry collections.
 * Adhering to CQRS principles, it provides a read-only gateway that decouples
 * external adapters from retrieval logic and underlying storage implementation,
 * ensuring high-performance data access in multi-tenant environments.</p>
 *
 * <p><b>Architectural Principles (Mission-Critical Pattern):</b></p>
 * <ul>
 * <li><b>Non-blocking:</b> Fully integrated into the Project Reactor pipeline for high-throughput stream processing.</li>
 * <li><b>CQRS Compliant:</b> Strictly enforces a separation between read (query) and write (command) operations.</li>
 * <li><b>Data Isolation:</b> Ensures that multi-tenant scoping is enforced on all retrieval operations.</li>
 * <li><b>Backpressure-Aware:</b> Supports reactive streams to maintain system stability during high-volume data egress.</li>
 * </ul>
 *
 * @version 1.0.0
 */
public interface ListHashesUseCase {

    /**
     * Orchestrates the paginated, reactive retrieval of hash token registries based on filter criteria.
     *
     * @param query The {@link ListHashesQuery} encapsulating tenant context, filter criteria, and pagination metadata.
     * @return A {@link Flux} emitting a stream of {@link HashToken} matching the criteria.
     * @throws NullPointerException if the provided query is null, preserving pipeline integrity (Fail-Fast).
     */
    @Nonnull
    Flux<HashToken> execute(@Nonnull ListHashesQuery query);
}