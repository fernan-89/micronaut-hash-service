package com.thinklab.application.command;

import io.micronaut.core.annotation.Introspected;
import jakarta.annotation.Nonnull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.Objects;

/**
 * Input Boundary: Command object representing the intent to reactivate an inactive cryptographic hash.
 * This record enforces strict input validation using Jakarta Bean Validation and functional
 * sanitization to ensure domain integrity and security compliance.
 *
 * <p><b>Architectural Rules:</b></p>
 * <ul>
 *     <li>Immutable: Guarantees state consistency throughout the reactive pipeline.</li>
 *     <li>Forensics & Compliance: Requires a business justification for reactivation.</li>
 *     <li>Fail-Fast: Validates syntax and constraints at the application edge.</li>
 * </ul>
 *
 * @param hashId   The unique system identifier of the HashToken to be reactivated.
 * @param executor The user or system account authorizing this action.
 * @param reason   The business justification for the reactivation (Essential for audit trails).
 */
@Introspected
public record ReactivateHashCommand(
        @NotBlank(message = "Hash ID is mandatory")
        @Pattern(regexp = "^[a-zA-Z0-9-]+$", message = "Hash ID contains invalid characters")
        String hashId,

        @NotBlank(message = "Executor identification is mandatory for auditing")
        @Size(max = 100, message = "Executor identification is too long")
        String executor,

        @NotBlank(message = "A reason for reactivation must be provided")
        @Size(min = 5, max = 500, message = "Reason must be between 5 and 500 characters")
        String reason
) {

    /**
     * Compact constructor for defensive programming and input sanitization.
     */
    public ReactivateHashCommand {
        Objects.requireNonNull(hashId, "hashId cannot be null");
        Objects.requireNonNull(executor, "executor cannot be null");
        Objects.requireNonNull(reason, "reason cannot be null");

        // Sanitization: Remove leading/trailing whitespaces to ensure data integrity
        hashId = hashId.trim();
        executor = executor.trim();
        reason = reason.trim();
    }
}