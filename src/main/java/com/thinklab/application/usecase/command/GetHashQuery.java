package com.thinklab.application.usecase.command;

import io.micronaut.core.annotation.Introspected;
import jakarta.annotation.Nonnull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import java.util.Objects;

/**
 * Input Boundary: Query object representing the intent to retrieve a specific cryptographic hash.
 * This record encapsulates the criteria for a read-only operation. By enforcing strict
 * input validation at the boundary, we prevent malformed identifiers from reaching
 * the persistence layer, mitigating potential NoSQL injection risks.
 *
 * <p><b>Architectural Rules:</b></p>
 * <ul>
 *     <li>Immutable: Ensures the query state remains constant throughout the pipeline.</li>
 *     <li>Fail-Fast: Validates syntactic constraints before resource consumption.</li>
 *     <li>Sanitized: Automatically removes leading/trailing spaces for consistent lookups.</li>
 * </ul>
 *
 * @param hashId The unique system identifier of the HashToken to be retrieved.
 */
@Introspected
public record GetHashQuery(
        @NotBlank(message = "Hash ID is mandatory for retrieval")
        @Pattern(regexp = "^[a-zA-Z0-9-]+$", message = "Hash ID contains invalid characters")
        String hashId
) {

    /**
     * Compact constructor for input sanitization and defensive programming.
     */
    public GetHashQuery {
        Objects.requireNonNull(hashId, "hashId cannot be null");

        // Sanitization: Ensure clean ID for database lookup
        hashId = hashId.trim();
    }
}