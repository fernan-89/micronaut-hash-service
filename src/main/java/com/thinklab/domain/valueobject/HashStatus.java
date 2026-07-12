package com.thinklab.domain.valueobject;

import com.thinklab.domain.exception.InvalidHashStatusException;
import jakarta.annotation.Nonnull;

import java.util.Objects;

/**
 * Value Object: Represents the lifecycle status of a HashToken entity.
 * This Enum acts as the central State Machine for the domain, enforcing strict
 * lifecycle transitions. It ensures that the HashToken cannot enter an invalid
 * state (e.g., reactivating a revoked token), maintaining audit and data integrity.
 *
 * <p><b>Lifecycle Rules:</b></p>
 * <ul>
 *     <li>ACTIVE: Initial state. Can be deactivated or revoked.</li>
 *     <li>INACTIVE: Paused state. Can be reactivated or revoked.</li>
 *     <li>REVOKED: Terminal state. No further transitions allowed (Zero Trust).</li>
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
     * Internal state machine logic using Java 21 switch expressions.
     *
     * @param targetStatus The desired state.
     * @return true if the transition is legally allowed.
     */
    public boolean canTransitionTo(@Nonnull HashStatus targetStatus) {
        return switch (this) {
            case ACTIVE -> targetStatus == INACTIVE || targetStatus == REVOKED;
            case INACTIVE -> targetStatus == ACTIVE || targetStatus == REVOKED;
            case REVOKED -> false; // Terminal state: Zero Trust enforced.
        };
    }
}