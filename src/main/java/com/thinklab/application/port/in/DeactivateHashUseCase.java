package com.thinklab.application.port.in;

import com.thinklab.application.usecase.command.DeactivateHashCommand;
import com.thinklab.domain.model.HashToken;
import jakarta.annotation.Nonnull;
import reactor.core.publisher.Mono;

/**
 * Input Port (UseCase): Defines the contract for deactivating an existing cryptographic hash registry.
 * This interface acts as a boundary between external adapters (e.g., REST Controllers) and the
 * core application logic, ensuring that the deactivation process is strictly governed by
 * domain rules and compliance requirements.
 *
 * <p><b>Contractual Obligations:</b></p>
 * <ul>
 *     <li>Implementation must be strictly non-blocking, returning a {@link Mono}.</li>
 *     <li>Must leverage the {@link com.thinklab.domain.model.HashToken} aggregate to
 *         validate state transition invariants.</li>
 *     <li>Must signal {@link com.thinklab.domain.exception.HashNotFoundException} if the
 *         requested identifier does not exist in the persistence layer.</li>
 *     <li>Must signal {@link com.thinklab.domain.exception.InvalidHashStatusException} if
 *         the transition is illegal (e.g., trying to deactivate a REVOKED token).</li>
 *     <li>Must ensure that a mandatory audit log is created containing the business
 *         justification (reason) provided in the command.</li>
 * </ul>
 */
public interface DeactivateHashUseCase {

    /**
     * Orchestrates the deactivation of a hash token, transitioning it to an INACTIVE state.
     *
     * @param command The immutable command containing the target hash ID, executor, and reason.
     * @return A {@link Mono} that emits the successfully deactivated {@link HashToken}.
     * @throws IllegalArgumentException if the command is null.
     */
    @Nonnull
    Mono<HashToken> execute(@Nonnull DeactivateHashCommand command);
}