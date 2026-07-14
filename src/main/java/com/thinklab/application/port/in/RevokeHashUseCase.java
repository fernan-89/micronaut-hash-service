package com.thinklab.application.port.in;

import com.thinklab.application.usecase.command.RevokeHashCommand;
import com.thinklab.domain.model.HashToken;
import jakarta.annotation.Nonnull;
import reactor.core.publisher.Mono;

/**
 * Application Port: Input boundary for the permanent revocation of an {@link HashToken}.
 * <p>This interface defines the use case contract for the irreversible transition of a
 * cryptographic hash registry to a terminal REVOKED state. Designed for Zero Trust
 * environments, it ensures that all revocation events are subject to strict business
 * validation and comprehensive forensic auditing.</p>
 *
 * <p><b>Architectural Principles (Mission-Critical Pattern):</b></p>
 * <ul>
 * <li><b>Non-blocking:</b> Fully integrated into the Project Reactor pipeline for high-throughput orchestration.</li>
 * <li><b>Terminal State:</b> Enforces irreversibility, ensuring tokens cannot transition out of the REVOKED status.</li>
 * <li><b>Audit-Mandatory:</b> Every revocation event triggers a mandatory forensic audit trail capturing business context and justification.</li>
 * <li><b>Defensive Error Handling:</b> Provides standardized signaling for missing entities or illegal state transitions to maintain stream integrity.</li>
 * </ul>
 *
 * @version 1.0.0
 */
public interface RevokeHashUseCase {

    /**
     * Orchestrates the irreversible revocation of a hash registry.
     *
     * @param command The {@link RevokeHashCommand} encapsulating the target identifier, executor, and mandatory business justification.
     * @return A {@link Mono} emitting the mutated {@link HashToken} in its terminal REVOKED state.
     * @throws NullPointerException if the provided command is null, preserving pipeline integrity (Fail-Fast).
     */
    @Nonnull
    Mono<HashToken> execute(@Nonnull RevokeHashCommand command);
}