package com.thinklab.application.port.in;

import com.thinklab.application.usecase.command.GenerateHashCommand;
import com.thinklab.domain.model.HashToken;
import jakarta.annotation.Nonnull;
import reactor.core.publisher.Mono;

/**
 * Application Port: Input boundary for the generation of a new cryptographic {@link HashToken}.
 * <p>This interface defines the use case contract for the creation and initialization of new
 * registry tokens. It acts as the primary entry point for external adapters, ensuring that
 * business rules, cryptographic invariants, and mandatory forensic auditing are enforced
 * strictly before state persistence.</p>
 *
 * <p><b>Architectural Principles (Mission-Critical Pattern):</b></p>
 * <ul>
 * <li><b>Non-blocking:</b> Fully integrated into the Project Reactor pipeline for high-throughput orchestration.</li>
 * <li><b>Domain Integrity:</b> Enforces cryptographic invariants and algorithm validation during the instantiation process.</li>
 * <li><b>Audit-Mandatory:</b> Every generation event triggers a forensic audit trail capturing the creation context and business justification.</li>
 * <li><b>Duplicate Prevention:</b> Implements pre-emptive validation checks to ensure uniqueness constraints within the tenant scope.</li>
 * </ul>
 *
 * @version 1.0.0
 */
public interface GenerateHashUseCase {

    /**
     * Orchestrates the creation, formatting, and atomic persistence of a new cryptographic registry.
     *
     * @param command The {@link GenerateHashCommand} encapsulating the tenant context, payload, and cryptographic algorithm.
     * @return A {@link Mono} emitting the successfully created and persisted {@link HashToken}.
     * @throws NullPointerException if the provided command is null, preserving pipeline integrity (Fail-Fast).
     */
    @Nonnull
    Mono<HashToken> execute(@Nonnull GenerateHashCommand command);
}