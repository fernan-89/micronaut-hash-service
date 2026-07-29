package com.thinklab.application.port.in;

import com.thinklab.application.usecase.command.RevokeHashCommand;
import com.thinklab.domain.model.HashToken;
import reactor.core.publisher.Mono;

/**
 * Application Port: Input boundary for the permanent revocation of an {@link HashToken}.
 *
 * <p><b>Architectural Role:</b>
 * This interface defines the primary input port (use case contract) for the irreversible transition of a
 * cryptographic hash registry to a terminal REVOKED state. Designed for Zero Trust architectures, it serves
 * as the formal boundary ensuring that all revocation events are subject to strict business rule validation,
 * terminal state invariants, and mandatory transactional forensic auditing.
 *
 * <p><b>Contractual Obligations:</b>
 * <ul>
 * <li><b>Reactive Non-Blocking Stream:</b> Operates strictly within the Project Reactor pipeline using {@link Mono},
 *     guaranteeing zero thread-blocking across Netty I/O channels.</li>
 * <li><b>Terminal State Invariance:</b> Enforces absolute irreversibility, ensuring tokens can never transition
 *     out of the terminal REVOKED operational status.</li>
 * <li><b>Audit-Mandatory:</b> Guarantees that every execution triggers the transactional generation of an
 *     immutable forensic audit log capturing justification and actor telemetry.</li>
 * <li><b>Framework Independence:</b> Completely decoupled from infrastructure frameworks, relying solely on
 *     native Java types and standard reactive primitives.</li>
 * </ul>
 *
 * @author ThinkLab
 * @version 1.0.0
 * @since 1.0
 */
public interface RevokeHashUseCase {

    /**
     * Orchestrates the secure, irreversible revocation of a hash registry.
     *
     * @param command The {@link RevokeHashCommand} encapsulating the target UUID, executor identity,
     *                and mandatory business justification. Must not be null.
     * @return A {@link Mono} emitting the mutated {@link HashToken} in its terminal REVOKED state.
     * @throws NullPointerException if the provided command is null.
     */
    Mono<HashToken> execute(RevokeHashCommand command);
}