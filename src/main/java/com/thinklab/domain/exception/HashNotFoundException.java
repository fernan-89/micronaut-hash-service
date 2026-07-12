package com.thinklab.domain.exception;

/**
 * Domain Exception thrown when a requested HashToken cannot be found.
 */
public class HashNotFoundException extends BusinessException {

    private static final String ERROR_CODE = "HASH_NOT_FOUND";

    public HashNotFoundException(String hashId) {
        super(ERROR_CODE, String.format("HashToken with ID [%s] was not found.", hashId));
    }
}
