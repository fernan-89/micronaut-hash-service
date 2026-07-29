package com.thinklab.domain.exception;

import java.util.Objects;

/**
 * Domain Layer: Abstract base class for all business rule and domain invariant violations.
 *
 * <p><b>Architectural Role:</b>
 * This exception serves as the ultimate parent for all intentional, recoverable business errors
 * within the Domain Layer. It acts as a clear demarcation line between logical business failures
 * (e.g., invalid state transitions, missing entities) and catastrophic infrastructure failures
 * (e.g., database timeouts, network partitions).
 *
 * <p><b>Contractual Obligations:</b>
 * <ul>
 *   <li><b>Abstract Enforcement:</b> This class is abstract to prevent generic, ambiguous errors.
 *       Developers must extend this to create highly specific exceptions (e.g., {@code HashNotFoundException},
 *       {@code StateConflictException}).</li>
 *   <li><b>RFC 7807 (Problem Details) Synergy:</b> Designed to be intercepted by the Application/Presentation
 *       layer's Exception Handlers (e.g., Micronaut {@code @Error} handlers). The {@code errorCode} maps
 *       to the RFC 7807 {@code type} or {@code title}, allowing automatic translation to HTTP 422, 409, or 404.</li>
 *   <li><b>Defensive Instantiation:</b> Rejects null or blank error codes and messages to ensure
 *       telemetry and observability streams are never poisoned with empty diagnostic data.</li>
 * </ul>
 *
 * <p><b>Reactive Behavior (Project Reactor):</b>
 * Within non-blocking flows, subclasses of this exception should be yielded via {@code Mono.error()}
 * or {@code Flux.error()}. They will safely propagate downstream to the subscriber (or global error handler)
 * without collapsing the Netty EventLoop.
 *
 * @author ThinkLab
 * @since 1.0
 */
public abstract class BusinessException extends RuntimeException {

    private final String errorCode;

    /**
     * Constructs a new BusinessException with the specified error code and detailed message.
     *
     * @param errorCode A unique, stable identifier for the error type (e.g., "ERR-HASH-001").
     *                  Used by the infrastructure layer to map to specific HTTP statuses.
     * @param message   A human-readable, technically accurate description of the violation.
     * @throws NullPointerException if {@code errorCode} or {@code message} is null.
     * @throws IllegalArgumentException if {@code errorCode} or {@code message} is blank.
     */
    protected BusinessException(String errorCode, String message) {
        super(validateMessage(message));
        this.errorCode = validateErrorCode(errorCode);
    }

    /**
     * Constructs a new BusinessException with the specified error code, detailed message, and root cause.
     *
     * @param errorCode A unique, stable identifier for the error type (e.g., "ERR-HASH-001").
     * @param message   A human-readable, technically accurate description of the violation.
     * @param cause     The underlying exception that triggered this domain violation.
     * @throws NullPointerException if {@code errorCode}, {@code message}, or {@code cause} is null.
     * @throws IllegalArgumentException if {@code errorCode} or {@code message} is blank.
     */
    protected BusinessException(String errorCode, String message, Throwable cause) {
        super(validateMessage(message), Objects.requireNonNull(cause, "Root cause cannot be null."));
        this.errorCode = validateErrorCode(errorCode);
    }

    /**
     * Retrieves the stable error code associated with this business violation.
     *
     * @return The strictly validated, non-null error code.
     */
    public String getErrorCode() {
        return errorCode;
    }

    /**
     * Internal defensive validation for the error code.
     */
    private static String validateErrorCode(String errorCode) {
        Objects.requireNonNull(errorCode, "Domain Exception constraint violated: errorCode cannot be null.");
        if (errorCode.isBlank()) {
            throw new IllegalArgumentException("Domain Exception constraint violated: errorCode cannot be blank.");
        }
        return errorCode;
    }

    /**
     * Internal defensive validation for the detail message.
     */
    private static String validateMessage(String message) {
        Objects.requireNonNull(message, "Domain Exception constraint violated: message cannot be null.");
        if (message.isBlank()) {
            throw new IllegalArgumentException("Domain Exception constraint violated: message cannot be blank.");
        }
        return message;
    }
}