package com.thinklab.application.port.in;

import com.thinklab.application.usecase.command.GenerateHashCommand;
import com.thinklab.domain.model.*;
import jakarta.annotation.Nonnull;
import reactor.core.publisher.Mono;

/**
 * Input Port (UseCase): Defines the contract for generating a new cryptographic hash or serial key.
 * This interface acts as the primary entry point for the generation business process,
 * ensuring that inbound adapters (e.g., REST Controllers) remain strictly decoupled
 * from the core domain logic and orchestration specifics.
 *
 * <p><b>Contractual Obligations:</b></p>
 * <ul>
 *     <li>Implementation must be strictly non-blocking, returning a {@link Mono}.</li>
 *     <li>Must leverage the {@link com.thinklab.domain.model.HashToken} aggregate to
 *         validate invariants and perform cryptographic operations.</li>
 *     <li>Must coordinate with Output Ports to persist the generated registry and
 *         its mandatory audit trail.</li>
 *     <li>Must handle duplicate detection according to business rules before creation.</li>
 * </ul>
 */
public interface GenerateHashUseCase {

    /**
     * Orchestrates the creation, formatting, and persistence of a new cryptographic registry.
     *
     * @param command The immutable command containing the tenant context, payload, and chosen algorithm.
     * @return A {@link Mono} that emits the successfully created and persisted {@link HashToken}.
     * @throws IllegalArgumentException if the command is null.
     */
    @Nonnull
    Mono<HashToken> execute(@Nonnull GenerateHashCommand command);
}