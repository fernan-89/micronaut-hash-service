package com.thinklab.application.usecase.command;

import io.micronaut.core.annotation.Introspected;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.extern.slf4j.Slf4j;

import java.util.Objects;
import java.util.UUID;

/**
 * Application Command: Encapsulates the intent to reactivate an INACTIVE cryptographic {@link com.thinklab.domain.model.HashToken}.
 *
 * <p><b>Architectural Role:</b>
 * This immutable command record serves as the formal request payload for cryptographic hash reactivation workflows.
 * It enforces strict declarative constraints via Jakarta Bean Validation and defensive runtime sanitization
 * to guarantee data integrity before entering the application core layer.
 *
 * <p><b>Contractual Obligations:</b>
 * <ul>
 * <li><b>Immutability:</b> Implemented as a Java record to guarantee thread-safe propagation across reactive streams.</li>
 * <li><b>Identity Sovereignty:</b> Enforces native {@link UUID} representation for target identifiers, aligning
 *     with BSON Binary (Subtype 4) persistence standards.</li>
 * <li><b>Forensic Accountability:</b> Mandates executor identity and a detailed business justification string
 *     to satisfy strict compliance and auditing standards.</li>
 * <li><b>Defensive Sanitization:</b> Compact constructor trims whitespace and executes fail-fast validation checks
 *     with structured logging hooks.</li>
 * </ul>
 *
 * @param hashId   The universally unique identifier (UUID) of the HashToken to be reactivated. Must not be null.
 * @param executor The principal identifier of the user or system authorizing this action. Must not be blank.
 * @param reason   The business justification provided for the operational restoration. Must be between 5 and 500 characters.
 *
 * @author ThinkLab
 * @version 1.0.0
 * @since 1.0
 */
@Slf4j
@Introspected
public record ReactivateHashCommand(
        @NotNull(message = "Hash ID UUID is mandatory")
        UUID hashId,

        @NotBlank(message = "Executor identification is mandatory for auditing")
        @Size(max = 100, message = "Executor identification is too long")
        String executor,

        @NotBlank(message = "A reason for reactivation must be provided")
        @Size(min = 5, max = 500, message = "Reason must be between 5 and 500 characters")
        String reason
) {

    /**
     * Compact constructor for defensive programming, input sanitization, and structured forensic logging.
     * Acts as the final gatekeeper for data integrity, ensuring inputs are normalized and invalid states
     * are intercepted immediately.
     */
    public ReactivateHashCommand {
        Objects.requireNonNull(hashId, "Application constraint violated: hashId UUID cannot be null.");
        Objects.requireNonNull(executor, "Application constraint violated: executor cannot be null.");
        Objects.requireNonNull(reason, "Application constraint violated: reason cannot be null.");

        // Normalization: Trim whitespace on string fields
        executor = executor.trim();
        reason = reason.trim();

        // Defense-in-depth: Programmatic validation check for non-web or direct instantiation contexts
        if (executor.isBlank() || reason.length() < 5) {
            log.error("[ACTION: REACTIVATE_HASH_VALIDATION] [ID: {}] [EXECUTOR: {}] - CRITICAL: Pipeline aborted due to malformed command input.", hashId, executor);
            throw new IllegalArgumentException("Application constraint violated: executor must not be blank and reason must provide sufficient context (min 5 characters).");
        }
    }
}