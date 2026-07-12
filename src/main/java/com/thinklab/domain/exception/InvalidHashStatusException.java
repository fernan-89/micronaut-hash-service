package com.thinklab.domain.exception;

/**
 * Domain Business Exception thrown when an illegal state transition is attempted
 * on a HashToken lifecycle (e.g., trying to activate a REVOKED token).
 */
public class InvalidHashStatusException extends BusinessException {

    private static final String ERROR_CODE = "INVALID_STATE_TRANSITION";

    public InvalidHashStatusException(String message) {
        super(ERROR_CODE, message);
    }
}