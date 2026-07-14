package com.thinklab.application.usecase.command;

import io.micronaut.core.annotation.Introspected;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.extern.slf4j.Slf4j;

import java.util.Objects;

/**
 * Application Command: Encapsulates the intent to permanently revoke a cryptographic {@link com.thinklab.domain.model.HashToken}.
 * <p>This immutable command object serves as the formal request structure for terminal revocation workflows.
 * Under the Zero Trust principle, this action is irreversible and requires mandatory business
 * justification, ensuring strict compliance with forensic audit requirements.</p>
 *
 * <p><b>Architectural Principles (Mission-Critical Pattern):</b></p>
 * <ul>
 * <li><b>Immutability:</b> Implemented as a Java record to ensure thread-safe, consistent state propagation.</li>
 * <li><b>Terminal State:</b> Formally documents the irreversible nature of the revocation action.</li>
 * <li><b>Forensic Integrity:</b> Requires mandatory executor and detailed justification attributes for compliance logging.</li>
 * <li><b>Edge Validation:</b> Combines Jakarta Bean Validation with defensive programming to sanitize and validate inputs at the boundary.</li>
 * </ul>
 *
 * @param hashId   The unique system identifier of the HashToken to be revoked.
 * @param executor The principal identifier of the user or system authorizing this terminal action.
 * @param reason   The mandatory business justification for the permanent revocation (Critical for security forensics).
 */
@Slf4j
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
     * Compact constructor for defensive programming, input sanitization, and forensic logging.
     * Acts as the final gatekeeper for data integrity, ensuring that transient inputs are
     * sanitized and invalid attempts are logged for security analysis.
     */
    public RevokeHashCommand {
        Objects.requireNonNull(hashId, "hashId cannot be null");
        Objects.requireNonNull(executor, "executor cannot be null");
        Objects.requireNonNull(reason, "reason cannot be null");

        // Sanitization: Normalize whitespace to prevent data contamination
        hashId = hashId.trim();
        executor = executor.trim();
        reason = reason.trim();

        // Defense-in-depth: Validation check for non-web instantiation contexts
        if (hashId.isBlank() || executor.isBlank() || reason.length() < 10) {
            log.error("[ACTION: REVOKE_HASH_VALIDATION] - CRITICAL: Pipeline aborted due to malformed command input. [ID: {}] [EXECUTOR: {}]", hashId, executor);
            throw new IllegalArgumentException("Invalid command state: hashId/executor must not be blank and reason must provide sufficient forensic context.");
        }
    }
}