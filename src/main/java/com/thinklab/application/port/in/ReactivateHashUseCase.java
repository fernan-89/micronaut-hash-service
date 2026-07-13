package com.thinklab.application.port.in;

import com.thinklab.application.usecase.command.ReactivateHashCommand;
import com.thinklab.domain.model.HashToken;
import jakarta.annotation.Nonnull;
import reactor.core.publisher.Mono;

/**
 * Input Port (UseCase): Defines the contract for restoring an INACTIVE hash token to ACTIVE status.
 * This interface acts as a boundary between external adapters and the internal application logic,
 * ensuring that the reactivation process is strictly governed by domain rules and compliance.
 *
 * <p><b>Contractual Obligations:</b></p>
 * <ul>
 *     <li>Implementation must be strictly non-blocking, returning a {@link Mono}.</li>
 *     <li>Must validate that the current state of the {@link HashToken} allows reactivation
 *         (e.g., transition from INACTIVE to ACTIVE).</li>
 *     <li>Must signal {@link com.thinklab.domain.exception.HashNotFoundException} if the
 *         requested identifier does not exist.</li>
 *     <li>Must signal {@link com.thinklab.domain.exception.InvalidHashStatusException} if
 *         the transition is illegal (e.g., trying to reactivate a REVOKED token).</li>
 *     <li>Must ensure that a mandatory audit log is created containing the business
 *         justification (reason) provided in the command.</li>
 * </ul>
 */
public interface ReactivateHashUseCase {

    /**
     * Orchestrates the restoration of a hash token's operational status.
     *
     * @param command The immutable command containing the hash ID, executor, and business reason.
     * @return A {@link Mono} that emits the successfully reactivated {@link HashToken}.
     * @throws IllegalArgumentException if the command is null.
     */
    @Nonnull
    Mono<HashToken> execute(@Nonnull ReactivateHashCommand command);
}