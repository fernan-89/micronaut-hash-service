package com.thinklab.application.port.in;

import com.thinklab.application.usecase.command.DeactivateHashCommand;
import com.thinklab.domain.model.HashToken;
import jakarta.annotation.Nonnull;
import reactor.core.publisher.Mono;

/**
 * Application Port: Input boundary for the deactivation of a {@link HashToken}.
 * <p>This interface defines the use case contract for transitioning an active cryptographic
 * hash registry into an INACTIVE state. It acts as the primary entry point for external
 * adapters, ensuring that business rules, state transition invariants, and mandatory
 * forensic auditing are enforced strictly before state persistence.</p>
 *
 * <p><b>Architectural Principles (Mission-Critical Pattern):</b></p>
 * <ul>
 * <li><b>Non-blocking:</b> Fully integrated into the Project Reactor pipeline for high-throughput orchestration.</li>
 * <li><b>State Integrity:</b> Enforces domain invariants to prevent illegal transitions (e.g., attempting to deactivate a REVOKED token).</li>
 * <li><b>Audit-Mandatory:</b> Every deactivation event triggers a forensic audit trail capturing business context and justification.</li>
 * <li><b>Defensive Error Handling:</b> Standardized signaling for missing entities or invalid state transitions to maintain stream integrity.</li>
 * </ul>
 *
 * @version 1.0.0
 */
public interface DeactivateHashUseCase {

    /**
     * Orchestrates the state transition of a hash token to an INACTIVE status.
     *
     * @param command The {@link DeactivateHashCommand} encapsulating the target identifier, executor, and business justification.
     * @return A {@link Mono} emitting the mutated {@link HashToken} in its new INACTIVE state.
     * @throws NullPointerException if the provided command is null, preserving pipeline integrity (Fail-Fast).
     */
    @Nonnull
    Mono<HashToken> execute(@Nonnull DeactivateHashCommand command);
}