package com.thinklab.domain.valueobject;

import com.thinklab.domain.exception.InvalidHashStatusException;
import jakarta.annotation.Nonnull;

import java.util.Objects;

/**
 * Domain Value Object: State machine representing the lifecycle of a HashToken entity.
 * <p>This enum enforces strict state transition rules, ensuring data integrity and
 * business compliance. It acts as the central gatekeeper, preventing illegal
 * operations on tokens based on their current lifecycle status.</p>
 *
 * <p><b>Architectural Principles (Mission-Critical Pattern):</b></p>
 * <ul>
 * <li><b>Deterministic State Machine:</b> Strictly defines valid and invalid transitions.</li>
 * <li><b>Compliance Enforcement:</b> Prevents unauthorized state shifts (e.g., reactivating revoked tokens).</li>
 * <li><b>Zero Trust:</b> Treats REVOKED as a terminal state with no exit path.</li>
 * </ul>
 */
public enum HashStatus {

    /**
     * Token is fully functional and available for validation.
     */
    ACTIVE,

    /**
     * Token is temporarily disabled. Can be reactivated to ACTIVE.
     */
    INACTIVE,

    /**
     * Token is permanently disabled (e.g., due to a security breach).
     * This is a terminal state.
     */
    REVOKED;

    /**
     * Validates if the transition to the target status is allowed.
     *
     * @param targetStatus The desired state to transition to.
     * @throws InvalidHashStatusException if the transition violates business rules.
     * @throws IllegalArgumentException if the targetStatus is null.
     */
    public void validateTransitionTo(@Nonnull HashStatus targetStatus) {
        Objects.requireNonNull(targetStatus, "Target status cannot be null.");

        if (this == targetStatus) {
            throw new InvalidHashStatusException(String.format(
                    "Idempotency Warning: The HashToken is already in the [%s] state.", this));
        }

        if (!canTransitionTo(targetStatus)) {
            throw new InvalidHashStatusException(String.format(
                    "Compliance Violation: Illegal state transition from [%s] to [%s].", this, targetStatus));
        }
    }

    /**
     * Evaluates if the transition to the target status is legally permitted.
     *
     * @param targetStatus The desired state.
     * @return true if the transition is allowed.
     */
    public boolean canTransitionTo(@Nonnull HashStatus targetStatus) {
        return switch (this) {
            case ACTIVE -> targetStatus == INACTIVE || targetStatus == REVOKED;
            case INACTIVE -> targetStatus == ACTIVE || targetStatus == REVOKED;
            case REVOKED -> false;
        };
    }
}