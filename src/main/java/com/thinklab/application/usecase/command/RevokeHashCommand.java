package com.thinklab.application.usecase.command;

import io.micronaut.core.annotation.Introspected;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.extern.slf4j.Slf4j;

import java.util.Objects;
import java.util.UUID;

/**
 * Application Command: Encapsulates the intent to permanently revoke a cryptographic {@link com.thinklab.domain.model.HashToken}.
 *
 * <p><b>Architectural Role:</b>
 * This immutable command record serves as the formal request payload for terminal revocation workflows.
 * Under Zero Trust principles, this action is irreversible and mandates rigorous business justification
 * and executor telemetry, ensuring strict compliance with forensic audit requirements.
 *
 * <p><b>Contractual Obligations:</b>
 * <ul>
 * <li><b>Immutability:</b> Implemented as a Java record to guarantee thread-safe propagation across reactive streams.</li>
 * <li><b>Identity Sovereignty:</b> Enforces native {@link UUID} representation for target identifiers, aligning
 *     with BSON Binary (Subtype 4) persistence standards.</li>
 * <li><b>Terminal State Integrity:</b> Formally encapsulates the irreversible nature of the revocation action.</li>
 * <li><b>Defensive Sanitization:</b> Compact constructor trims whitespace and executes fail-fast validation checks
 *     with structured logging hooks.</li>
 * </ul>
 *
 * @param hashId   The universally unique identifier (UUID) of the HashToken to be revoked. Must not be null.
 * @param executor The principal identifier of the user or system authorizing this terminal action. Must not be blank.
 * @param reason   The mandatory business justification for the permanent revocation (between 10 and 1000 characters). Must not be blank.
 *
 * @author ThinkLab
 * @version 1.0.0
 * @since 1.0
 */
@Slf4j
@Introspected
public record RevokeHashCommand(
        @NotNull(message = "Hash ID UUID is mandatory")
        UUID hashId,

        @NotBlank(message = "Executor identification is mandatory for auditing")
        @Size(max = 100, message = "Executor identification is too long")
        String executor,

        @NotBlank(message = "A valid reason for revocation must be provided for compliance")
        @Size(min = 10, max = 1000, message = "Reason must be between 10 and 1000 characters")
        String reason
) {

    /**
     * Compact constructor for defensive programming, input sanitization, and structured forensic logging.
     * Acts as the final gatekeeper for data integrity, ensuring inputs are normalized and invalid states
     * are intercepted immediately.
     */
    public RevokeHashCommand {
        Objects.requireNonNull(hashId, "Application constraint violated: hashId UUID cannot be null.");
        Objects.requireNonNull(executor, "Application constraint violated: executor cannot be null.");
        Objects.requireNonNull(reason, "Application constraint violated: reason cannot be null.");

        // Normalization: Trim whitespace on string fields
        executor = executor.trim();
        reason = reason.trim();

        // Defense-in-depth: Programmatic validation check for non-web or direct instantiation contexts
        if (executor.isBlank() || reason.length() < 10) {
            log.error("[ACTION: REVOKE_HASH_VALIDATION] [ID: {}] [EXECUTOR: {}] - CRITICAL: Pipeline aborted due to malformed command input.", hashId, executor);
            throw new IllegalArgumentException("Application constraint violated: executor must not be blank and reason must provide sufficient forensic context (min 10 characters).");
        }
    }
}