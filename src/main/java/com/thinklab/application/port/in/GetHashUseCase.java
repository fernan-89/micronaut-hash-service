package com.thinklab.application.port.in;

import com.thinklab.application.usecase.command.GetHashQuery;
import com.thinklab.domain.model.HashToken;
import reactor.core.publisher.Mono;

/**
 * Application Port: Input boundary for the retrieval of a cryptographic {@link HashToken}.
 *
 * <p><b>Architectural Role:</b>
 * This interface defines the primary input port (use case contract) for querying the registry state.
 * Adhering strictly to CQRS (Command Query Responsibility Segregation) principles, it provides a dedicated
 * read-only gateway that decouples external inbound adapters from retrieval logic and underlying storage
 * implementations, ensuring high-performance data access.
 *
 * <p><b>Contractual Obligations:</b>
 * <ul>
 * <li><b>Reactive Non-Blocking Stream:</b> Operates strictly within the Project Reactor pipeline using {@link Mono},
 *     guaranteeing zero thread-blocking across Netty I/O channels.</li>
 * <li><b>CQRS Segregation:</b> Enforces a strict architectural barrier between read queries and write commands.</li>
 * <li><b>Resilient Error Handling:</b> Materializes standardized domain exceptions (such as {@link com.thinklab.domain.exception.HashNotFoundException})
 *     through the reactive stream when target aggregates are absent.</li>
 * <li><b>Framework Independence:</b> Completely decoupled from infrastructure frameworks, relying solely on
 *     native Java types and standard reactive primitives.</li>
 * </ul>
 *
 * @author ThinkLab
 * @version 1.0.0
 * @since 1.0
 */
public interface GetHashUseCase {

    /**
     * Orchestrates the secure retrieval of a hash token registry based on specific query criteria.
     *
     * @param query The {@link GetHashQuery} encapsulating the target UUID search parameter and security context. Must not be null.
     * @return A {@link Mono} emitting the requested {@link HashToken} if found, or an error signal if absent.
     * @throws NullPointerException if the provided query is null.
     */
    Mono<HashToken> execute(GetHashQuery query);
}