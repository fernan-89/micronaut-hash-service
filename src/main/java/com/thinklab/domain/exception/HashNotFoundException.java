package com.thinklab.domain.exception;

import com.thinklab.domain.model.HashToken;

import java.util.Objects;
import java.util.UUID;

/**
 * Domain Exception: Indicates that a requested {@link HashToken} could not be resolved from the repository.
 *
 * <p><b>Architectural Role:</b>
 * This exception signifies a strict logical failure where an operation mandates the existence
 * of a specific Aggregate Root, but the underlying persistence layer yields no result.
 *
 * <p><b>RFC 7807 (Problem Details) Synergy:</b>
 * The Presentation Layer's exception handlers must map the {@code ERROR_CODE} ({@value ERROR_CODE})
 * strictly to an <b>HTTP 404 (Not Found)</b>.
 *
 * <p><b>Reactive Behavior (Project Reactor):</b>
 * In a non-blocking persistence pipeline, repositories typically return a {@code Mono<HashToken>}.
 * If the record is absent, the repository emits an empty signal. The Application Service must intercept
 * this empty signal using {@code .switchIfEmpty(Mono.error(() -> new HashNotFoundException(id)))}
 * to propagate this business exception downstream.
 *
 * @author ThinkLab
 * @since 1.0
 */
public class HashNotFoundException extends BusinessException {

    /**
     * Standardized error code mapping to HTTP 404 Not Found.
     */
    private static final String ERROR_CODE = "ERR-HASH-00404";

    /**
     * Constructs a new HashNotFoundException for the missing deterministic identity.
     *
     * <p><b>Contract:</b> Defensively guarantees that the requested UUID is not null
     * before constructing the error message.
     *
     * @param id The universally unique identifier (UUID) of the requested {@link HashToken}.
     *           Must not be null.
     * @throws NullPointerException if the provided {@code id} is null.
     */
    public HashNotFoundException(UUID id) {
        super(
                ERROR_CODE,
                String.format("HashToken with deterministic ID [%s] could not be found in the system of record.",
                        Objects.requireNonNull(id, "Domain Exception constraint violated: UUID cannot be null."))
        );
    }
}