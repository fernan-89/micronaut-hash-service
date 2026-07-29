package com.thinklab.application.port.in;

import com.thinklab.application.usecase.command.ReactivateHashCommand;
import com.thinklab.domain.model.HashToken;
import reactor.core.publisher.Mono;

/**
 * Application Port: Input boundary for the reactivation (restoration) of an {@link HashToken}.
 *
 * <p><b>Architectural Role:</b>
 * This interface defines the primary input port (use case contract) for restoring an INACTIVE
 * cryptographic hash registry back to an ACTIVE operational state. It serves as the formal boundary
 * between external inbound adapters (such as HTTP controllers) and the core application layer, ensuring
 * that state machine invariants and mandatory forensic auditing are strictly enforced prior to persistence.
 *
 * <p><b>Contractual Obligations:</b>
 * <ul>
 * <li><b>Reactive Non-Blocking Stream:</b> Operates strictly within the Project Reactor pipeline using {@link Mono},
 *     guaranteeing zero thread-blocking across Netty I/O channels.</li>
 * <li><b>State Integrity & Invariant Enforcement:</b> Delegates state validation to the domain aggregate,
 *     preventing illegal transitions (e.g., attempting to reactivate a terminally revoked token).</li>
 * <li><b>Audit-Mandatory:</b> Guarantees that every execution triggers the transactional generation of an
 *     immutable forensic audit log capturing business justification and actor metadata.</li>
 * <li><b>Framework Independence:</b> Completely decoupled from infrastructure frameworks, relying solely on
 *     native Java types and standard reactive primitives.</li>
 * </ul>
 *
 * @author ThinkLab
 * @version 1.0.0
 * @since 1.0
 */
public interface ReactivateHashUseCase {

    /**
     * Orchestrates the secure state transition of an inactive hash token back to an ACTIVE operational status.
     *
     * @param command The {@link ReactivateHashCommand} encapsulating the target UUID, executor identity,
     *                and business justification. Must not be null.
     * @return A {@link Mono} emitting the mutated {@link HashToken} in its restored ACTIVE state.
     * @throws NullPointerException if the provided command is null.
     */
    Mono<HashToken> execute(ReactivateHashCommand command);
}