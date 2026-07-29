package com.thinklab.domain.exception;

import com.thinklab.domain.valueobject.HashStatus;

/**
 * Domain Exception: Indicates an illegal or unpermitted lifecycle state transition attempt
 * on a {@link com.thinklab.domain.model.HashToken}.
 *
 * <p><b>Architectural Role:</b>
 * This exception acts as the defensive enforcement arm of the {@link HashStatus} state machine.
 * It is thrown instantaneously when a client or internal process attempts to violate business
 * compliance rules, such as attempting to reactivate a token that has reached the terminal
 * {@code REVOKED} state, or requesting a redundant (idempotent) transition.
 *
 * <p><b>RFC 7807 (Problem Details) Synergy:</b>
 * The Presentation Layer's exception handlers must map the {@code ERROR_CODE} ({@value ERROR_CODE})
 * strictly to an <b>HTTP 409 (Conflict)</b> or <b>HTTP 422 (Unprocessable Entity)</b> response,
 * communicating a business rule violation rather than a technical failure.
 *
 * <p><b>Reactive Behavior (Project Reactor):</b>
 * Because state machine validations are pure, CPU-bound functions, this exception is typically
 * instantiated and thrown synchronously within non-blocking operators (e.g., {@code .map()} or
 * {@code .doOnNext()}). Reactor will automatically intercept the synchronous throw and route it
 * downstream as an {@code onError} signal, maintaining the integrity of the EventLoop.
 *
 * @author ThinkLab
 * @since 1.0
 */
public class InvalidHashStatusException extends BusinessException {

    /**
     * Standardized error code mapping to HTTP 409 Conflict (Business Rule Violation).
     */
    private static final String ERROR_CODE = "ERR-HASH-00409";

    /**
     * Constructs a new InvalidHashStatusException with a detailed explanation of the violation.
     *
     * <p><b>Contract:</b> The provided message should precisely detail the current state
     * and the illegal target state to aid in observability and client-side debugging.
     *
     * @param message A human-readable, technically accurate description of the state transition failure.
     *                Must not be null or blank (enforced by the superclass).
     */
    public InvalidHashStatusException(String message) {
        super(ERROR_CODE, message);
    }
}