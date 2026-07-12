package com.thinklab.application.command;

import io.micronaut.core.annotation.Introspected;
import jakarta.annotation.Nonnull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.Objects;

/**
 * Input Boundary: Command object representing the intent to permanently revoke a cryptographic hash.
 * This record enforces strict input validation using Jakarta Bean Validation and functional
 * sanitization. Revocation is a terminal state (Zero Trust), thus requiring a mandatory
 * business justification for security compliance.
 *
 * <p><b>Architectural Rules:</b></p>
 * <ul>
 *     <li>Immutable: Ensures state consistency across the reactive execution pipeline.</li>
 *     <li>Terminal Action: Requires an explicit reason for forensics and auditability.</li>
 *     <li>Fail-Fast: Validates syntactic and business constraints at the application boundary.</li>
 * </ul>
 *
 * @param hashId   The unique system identifier of the HashToken to be revoked.
 * @param executor The user or system account authorizing this terminal action.
 * @param reason   The mandatory business justification for the permanent revocation.
 */
@Introspected
public record RevokeHashCommand(
        @NotBlank(message = "Hash ID is mandatory")
        @Pattern(regexp = "^[a-zA-Z0-9-]+$", message = "Hash ID contains invalid characters")
        String hashId,

        @NotBlank(message = "Executor identification is mandatory for auditing")
        @Size(max = 100, message = "Executor identification is too long")
        String executor,

        @NotBlank(message = "A valid reason for revocation must be provided for compliance")
        @Size(min = 10, max = 1000, message = "Reason must be between 10 and 1000 characters")
        String reason
) {

    /**
     * Compact constructor for defensive programming and input sanitization.
     * Prevents leading/trailing whitespaces from polluting audit logs or breaking lookups.
     */
    public RevokeHashCommand {
        Objects.requireNonNull(hashId, "hashId cannot be null");
        Objects.requireNonNull(executor, "executor cannot be null");
        Objects.requireNonNull(reason, "reason cannot be null");

        // Sanitization: Trimming fields to ensure data integrity
        hashId = hashId.trim();
        executor = executor.trim();
        reason = reason.trim();
    }
}