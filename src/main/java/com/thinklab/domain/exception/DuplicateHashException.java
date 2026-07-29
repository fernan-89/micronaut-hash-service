package com.thinklab.domain.exception;

/**
 * Domain Exception: Thrown when an attempt is made to create a cryptographic hash token
 * that already exists for a given tenant and payload context.
 *
 * <p><b>Architectural Role:</b>
 * This exception represents a business rule validation failure (mapping to an HTTP 409 Conflict status)
 * preventing duplicate data pollution and maintaining data integrity within the domain layer.
 *
 * <p><b>Contractual Obligations:</b>
 * <ul>
 * <li><b>Domain Isolation:</b> Inherits from {@link BusinessException} to cleanly separate business
 *     logic violations from technical infrastructure errors.</li>
 * <li><b>Error Code Traceability:</b> Carries a deterministic error code for client-facing telemetry and auditing.</li>
 * </ul>
 *
 * @author ThinkLab
 * @version 1.0.0
 * @since 1.0
 */
public class DuplicateHashException extends BusinessException {

    private static final String DEFAULT_ERROR_CODE = "ERR-HASH-00409";

    /**
     * Constructs a new DuplicateHashException with a custom error code and detail message.
     *
     * @param errorCode The unique system error code. Must not be blank.
     * @param message   The detail message explaining the business constraint violation. Must not be blank.
     */
    public DuplicateHashException(String errorCode, String message) {
        super(errorCode, message);
    }

    /**
     * Constructs a new DuplicateHashException with the standard default error code ("ERR-HASH-00409")
     * and a specified detail message.
     *
     * @param message The detail message explaining the business constraint violation. Must not be blank.
     */
    public DuplicateHashException(String message) {
        super(DEFAULT_ERROR_CODE, message);
    }
}