package com.thinklab.application.port.in;

import com.thinklab.application.usecase.command.GenerateHashCommand;
import com.thinklab.domain.model.HashToken;
import reactor.core.publisher.Mono;

/**
 * Application Port: Input boundary for the generation of a new cryptographic {@link HashToken}.
 *
 * <p><b>Architectural Role:</b>
 * This interface defines the primary input port (use case contract) for the creation, cryptographic
 * computation, and initialization of new hash registry tokens. It serves as the formal boundary between
 * external inbound adapters (such as HTTP controllers) and the core application layer, ensuring that
 * business rules, uniqueness constraints, cryptographic invariants, and mandatory forensic auditing
 * are strictly enforced prior to persistence.
 *
 * <p><b>Contractual Obligations:</b>
 * <ul>
 * <li><b>Reactive Non-Blocking Stream:</b> Operates strictly within the Project Reactor pipeline using {@link Mono},
 *     guaranteeing zero thread-blocking across Netty I/O channels.</li>
 * <li><b>Duplicate Prevention:</b> Enforces pre-emptive uniqueness validation within the isolated tenant
 *     scope to prevent colliding payload registrations.</li>
 * <li><b>Audit-Mandatory:</b> Guarantees that every execution triggers the transactional generation of an
 *     immutable forensic audit log capturing creation telemetry.</li>
 * <li><b>Framework Independence:</b> Completely decoupled from infrastructure frameworks, relying solely on
 *     native Java types and standard reactive primitives.</li>
 * </ul>
 *
 * @author ThinkLab
 * @version 1.0.0
 * @since 1.0
 */
public interface GenerateHashUseCase {

    /**
     * Orchestrates the secure creation, computational formatting, and atomic persistence of a new cryptographic registry.
     *
     * @param command The {@link GenerateHashCommand} encapsulating the tenant context, payload, algorithm selection,
     *                and executor metadata. Must not be null.
     * @return A {@link Mono} emitting the successfully created, computed, and persisted {@link HashToken}.
     * @throws NullPointerException if the provided command is null.
     */
    Mono<HashToken> execute(GenerateHashCommand command);
}