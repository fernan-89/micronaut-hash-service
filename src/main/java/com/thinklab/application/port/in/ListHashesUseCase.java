package com.thinklab.application.port.in;

import com.thinklab.application.usecase.command.ListHashesQuery;
import com.thinklab.domain.model.HashToken;
import reactor.core.publisher.Flux;

/**
 * Application Port: Input boundary for the paginated retrieval of {@link HashToken} registries.
 *
 * <p><b>Architectural Role:</b>
 * This interface defines the primary input port (use case contract) for querying paginated registry collections.
 * Adhering strictly to CQRS (Command Query Responsibility Segregation) principles, it provides a dedicated read-only
 * gateway that decouples external inbound adapters from retrieval logic and underlying storage mechanisms,
 * ensuring high-performance data access in multi-tenant environments.
 *
 * <p><b>Contractual Obligations:</b>
 * <ul>
 * <li><b>Reactive Non-Blocking Stream:</b> Delivers paginated items asynchronously via Project Reactor {@link Flux},
 *     supporting backpressure and safeguarding Netty EventLoops during high-volume data egress.</li>
 * <li><b>CQRS Segregation:</b> Enforces a strict architectural barrier between read queries and write commands.</li>
 * <li><b>Tenant Data Isolation:</b> Guarantees that multi-tenant boundary scoping is strictly enforced across
 *     all query parameters.</li>
 * <li><b>Framework Independence:</b> Completely decoupled from infrastructure frameworks, relying solely on
 *     native Java types and standard reactive primitives.</li>
 * </ul>
 *
 * @author ThinkLab
 * @version 1.0.0
 * @since 1.0
 */
public interface ListHashesUseCase {

    /**
     * Orchestrates the paginated, reactive retrieval of hash token registries based on filter criteria.
     *
     * @param query The {@link ListHashesQuery} encapsulating tenant context, filter criteria, and pagination metadata. Must not be null.
     * @return A {@link Flux} emitting a backpressure-aware stream of matching {@link HashToken} domain aggregates.
     * @throws NullPointerException if the provided query is null.
     */
    Flux<HashToken> execute(ListHashesQuery query);
}