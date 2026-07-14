package com.thinklab.application.port.in;

import com.thinklab.application.usecase.command.ReactivateHashCommand;
import com.thinklab.domain.model.HashToken;
import jakarta.annotation.Nonnull;
import reactor.core.publisher.Mono;

/**
 * Application Port: Input boundary for the reactivation of an {@link HashToken}.
 * <p>This interface defines the use case contract for restoring a cryptographic hash
 * registry from an INACTIVE status back to an ACTIVE state. It ensures that state
 * transitions strictly adhere to business invariants and that every restoration event
 * is captured within a mandatory forensic audit trail.</p>
 *
 * <p><b>Architectural Principles (Mission-Critical Pattern):</b></p>
 * <ul>
 * <li><b>Non-blocking:</b> Fully integrated into the Project Reactor pipeline for high-throughput orchestration.</li>
 * <li><b>State Integrity:</b> Validates domain invariants to prevent illegal transitions (e.g., preventing the reactivation of a REVOKED token).</li>
 * <li><b>Audit-Mandatory:</b> Every reactivation event triggers a forensic audit trail capturing business context and justification.</li>
 * <li><b>Defensive Error Handling:</b> Provides standardized signaling for missing entities or invalid state transitions to maintain stream integrity.</li>
 * </ul>
 *
 * @version 1.0.0
 */
public interface ReactivateHashUseCase {

    /**
     * Orchestrates the state transition of a hash token back to an ACTIVE status.
     *
     * @param command The {@link ReactivateHashCommand} encapsulating the target identifier, executor, and business justification.
     * @return A {@link Mono} emitting the mutated {@link HashToken} in its restored ACTIVE state.
     * @throws NullPointerException if the provided command is null, preserving pipeline integrity (Fail-Fast).
     */
    @Nonnull
    Mono<HashToken> execute(@Nonnull ReactivateHashCommand command);
}