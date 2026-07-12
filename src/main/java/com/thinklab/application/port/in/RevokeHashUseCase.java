package com.thinklab.application.port.in;

import com.thinklab.application.command.RevokeHashCommand;
import com.thinklab.domain.model.HashToken;
import jakarta.annotation.Nonnull;
import reactor.core.publisher.Mono;

/**
 * Input Port (UseCase): Defines the contract for permanently revoking a cryptographic hash or serial key.
 * This interface represents the terminal entry point in the token lifecycle. Revocation is
 * an irreversible action under the Zero Trust principle, ensuring that once a token is
 * revoked, it cannot be transitioned back to any other state.
 *
 * <p><b>Contractual Obligations:</b></p>
 * <ul>
 *     <li>Implementation must be strictly non-blocking, returning a {@link Mono}.</li>
 *     <li>Must validate the transition using the {@link com.thinklab.domain.valueobject.HashStatus} state machine.</li>
 *     <li>Must signal {@link com.thinklab.domain.exception.HashNotFoundException} if the target ID does not exist.</li>
 *     <li>Must ensure that the revocation reason and executor identity are recorded in the audit trail.</li>
 *     <li>Should signal {@link com.thinklab.domain.exception.InvalidHashStatusException} for illegal state transitions.</li>
 * </ul>
 */
public interface RevokeHashUseCase {

    /**
     * Orchestrates the permanent and irreversible revocation of a hash registry.
     *
     * @param command The immutable command containing the target hash ID, executor, and mandatory reason.
     * @return A {@link Mono} that emits the revoked {@link HashToken} upon successful transition.
     * @throws IllegalArgumentException if the command is null.
     */
    @Nonnull
    Mono<HashToken> execute(@Nonnull RevokeHashCommand command);
}