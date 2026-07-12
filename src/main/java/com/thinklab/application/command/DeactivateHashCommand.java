package com.thinklab.application.command;

import io.micronaut.core.annotation.Introspected;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.Objects;

/**
 * Input Boundary: Command object representing the intent to deactivate an existing cryptographic hash.
 * This record enforces strict input validation using Jakarta Bean Validation and functional
 * sanitization to ensure domain integrity and security compliance.
 *
 * <p><b>Architectural Rules:</b></p>
 * <ul>
 *     <li><b>Immutable:</b> Guarantees state consistency throughout the reactive pipeline.</li>
 *     <li><b>Audit-Ready:</b> Requires an executor and a business reason for lifecycle transitions.</li>
 *     <li><b>Fail-Fast:</b> Validates syntax and constraints at the application edge.</li>
 * </ul>
 *
 * <p><b>Invariants:</b></p>
 * <ul>
 *     <li>hashId must be non-null and follow the alphanumeric pattern.</li>
 *     <li>executor must be non-null and properly identified.</li>
 *     <li>reason must be between 5 and 500 characters for forensic traceability.</li>
 * </ul>
 *
 * @param hashId   The unique system identifier of the HashToken to be deactivated.
 * @param executor The user or system account authorizing this action.
 * @param reason   The business justification for the deactivation (Critical for security forensics).
 */
@Introspected
public record DeactivateHashCommand(
        @NotBlank(message = "Hash ID is mandatory")
        @Pattern(regexp = "^[a-zA-Z0-9-]+$", message = "Hash ID contains invalid characters")
        String hashId,

        @NotBlank(message = "Executor identification is mandatory for auditing")
        @Size(max = 100, message = "Executor identification is too long")
        String executor,

        @NotBlank(message = "A reason for deactivation must be provided")
        @Size(min = 5, max = 500, message = "Reason must be between 5 and 500 characters")
        String reason
) {

    /**
     * Compact constructor for defensive programming and input sanitization.
     * Ensures that accidental white spaces do not pollute the database or break lookups.
     * This constructor acts as the final gatekeeper for data integrity in memory.
     */
    public DeactivateHashCommand {
        Objects.requireNonNull(hashId, "hashId cannot be null");
        Objects.requireNonNull(executor, "executor cannot be null");
        Objects.requireNonNull(reason, "reason cannot be null");

        // Sanitization: Remove leading/trailing whitespaces (Higienização)
        hashId = hashId.trim();
        executor = executor.trim();
        reason = reason.trim();

        // Defense-in-depth: Manual validation for non-web instantiation (e.g., Unit Tests)
        if (hashId.isBlank()) throw new IllegalArgumentException("hashId cannot be blank");
        if (executor.isBlank()) throw new IllegalArgumentException("executor cannot be blank");
        if (reason.length() < 5) throw new IllegalArgumentException("Reason must provide sufficient business context");
    }
}