package com.thinklab.application.usecase.command;

import io.micronaut.core.annotation.Introspected;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.extern.slf4j.Slf4j;

import java.util.Objects;

/**
 * Application Command: Encapsulates the intent to deactivate an existing cryptographic {@link com.thinklab.domain.model.HashToken}.
 * <p>This immutable command object serves as the formal request structure for deactivation workflows.
 * It enforces strict input validation, ensures data sanitization, and carries the necessary
 * forensic metadata required for compliance and audit logging.</p>
 *
 * <p><b>Architectural Principles (Mission-Critical Pattern):</b></p>
 * <ul>
 * <li><b>Immutability:</b> Implemented as a Java record to ensure thread-safe, consistent state propagation.</li>
 * <li><b>Forensic Integrity:</b> Requires mandatory executor and justification attributes to ensure accountability.</li>
 * <li><b>Edge Validation:</b> Combines Jakarta Bean Validation for declarative constraints with defensive programming for runtime sanitization and logging.</li>
 * </ul>
 *
 * @param hashId   The unique system identifier of the HashToken to be deactivated.
 * @param executor The principal identifier of the user or system authorizing this action.
 * @param reason   The business justification provided for the deactivation (Critical for security forensics).
 */
@Slf4j
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
     * Compact constructor for defensive programming, input sanitization, and forensic logging.
     * Acts as the final gatekeeper for data integrity, ensuring that transient inputs are
     * sanitized and invalid attempts are logged for security analysis.
     */
    public DeactivateHashCommand {
        Objects.requireNonNull(hashId, "hashId cannot be null");
        Objects.requireNonNull(executor, "executor cannot be null");
        Objects.requireNonNull(reason, "reason cannot be null");

        // Sanitization: Normalize whitespace to prevent data contamination
        hashId = hashId.trim();
        executor = executor.trim();
        reason = reason.trim();

        // Defense-in-depth: Validation check for non-web instantiation contexts
        if (hashId.isBlank() || executor.isBlank() || reason.length() < 5) {
            log.error("[ACTION: DEACTIVATE_HASH_VALIDATION] - CRITICAL: Pipeline aborted due to malformed command input. [ID: {}] [EXECUTOR: {}]", hashId, executor);
            throw new IllegalArgumentException("Invalid command state: hashId/executor must not be blank and reason must provide sufficient context.");
        }
    }
}