package com.thinklab.application.usecase.command;

import io.micronaut.core.annotation.Introspected;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.extern.slf4j.Slf4j;

import java.util.Objects;

/**
 * Application Query: Encapsulates the criteria for retrieving a specific cryptographic {@link com.thinklab.domain.model.HashToken}.
 * <p>This immutable query object serves as the formal request structure for read-only operations.
 * Following CQRS principles, it ensures strict input validation at the boundary, mitigating
 * risks such as NoSQL injection and ensuring that only syntactically valid identifiers
 * traverse into the domain layer.</p>
 *
 * <p><b>Architectural Principles (Mission-Critical Pattern):</b></p>
 * <ul>
 * <li><b>Immutability:</b> Implemented as a Java record to ensure thread-safe, consistent query propagation.</li>
 * <li><b>CQRS Compliant:</b> Strictly facilitates the read-side of the architecture, keeping logic separated from state mutation commands.</li>
 * <li><b>Edge Validation:</b> Combines Jakarta Bean Validation with defensive programming to sanitize and validate inputs before resource consumption.</li>
 * <li><b>Security-First:</b> Sanitizes input to prevent identifier contamination and potential injection vectors.</li>
 * </ul>
 *
 * @param hashId The unique system identifier of the HashToken to be retrieved.
 */
@Slf4j
@Introspected
public record GetHashQuery(
        @NotBlank(message = "Hash ID is mandatory for retrieval")
        @Pattern(regexp = "^[a-zA-Z0-9-]+$", message = "Hash ID contains invalid characters")
        String hashId
) {

    /**
     * Compact constructor for input sanitization, defensive programming, and forensic logging.
     * Acts as the final gatekeeper for data integrity, ensuring that invalid retrieval
     * attempts are caught and logged at the application edge.
     */
    public GetHashQuery {
        Objects.requireNonNull(hashId, "hashId cannot be null");

        // Sanitization: Normalize whitespace to ensure consistent lookups
        hashId = hashId.trim();

        // Defense-in-depth: Validation check for non-web instantiation contexts
        if (hashId.isBlank()) {
            log.error("[ACTION: GET_HASH_VALIDATION] - CRITICAL: Pipeline aborted due to malformed query input. [ID: BLANK]");
            throw new IllegalArgumentException("hashId cannot be blank");
        }
    }
}