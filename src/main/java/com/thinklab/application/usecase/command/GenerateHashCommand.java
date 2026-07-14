package com.thinklab.application.usecase.command;

import com.thinklab.domain.valueobject.HashAlgorithm;
import io.micronaut.core.annotation.Introspected;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.extern.slf4j.Slf4j;

import java.util.Objects;

/**
 * Application Command: Encapsulates the intent to generate a new cryptographic {@link com.thinklab.domain.model.HashToken}.
 * <p>This immutable command object serves as the formal request structure for generation workflows.
 * It enforces strict input validation, prevents memory exhaustion (DoS mitigation),
 * and ensures that all generation requests carry the required metadata for auditability.</p>
 *
 * <p><b>Architectural Principles (Mission-Critical Pattern):</b></p>
 * <ul>
 * <li><b>Immutability:</b> Implemented as a Java record to ensure thread-safe, consistent state propagation.</li>
 * <li><b>OOM Prevention:</b> Enforces strict payload sizing to ensure system stability under load.</li>
 * <li><b>Edge Validation:</b> Combines Jakarta Bean Validation with defensive programming to catch malformed requests at the boundary.</li>
 * <li><b>Forensic Integrity:</b> Requires mandatory service and executor identification to ensure accountability.</li>
 * </ul>
 *
 * @param tenantId      The unique identifier of the tenant requesting the generation.
 * @param payload       The raw data to be hashed (Limited for security and performance).
 * @param algorithm     The cryptographic algorithm chosen for this operation.
 * @param sourceService The name of the microservice or system invoking this action.
 * @param executor      The user or system account executing the action.
 * @param asSerialKey   Flag indicating if the output should be formatted as a serial key.
 */
@Slf4j
@Introspected
public record GenerateHashCommand(
        @NotBlank(message = "Tenant ID is mandatory")
        @Pattern(regexp = "^[a-zA-Z0-9-]+$", message = "Tenant ID contains invalid characters")
        String tenantId,

        @NotBlank(message = "Payload is mandatory")
        @Size(max = 10000, message = "Payload size exceeds security limit to prevent OOM")
        String payload,

        @NotNull(message = "Hash algorithm is mandatory")
        HashAlgorithm algorithm,

        @NotBlank(message = "Source service is mandatory")
        @Size(max = 50, message = "Source service name is too long")
        String sourceService,

        @NotBlank(message = "Executor identification is mandatory for auditing")
        @Size(max = 100, message = "Executor identification is too long")
        String executor,

        @NotNull
        Boolean asSerialKey
) {

    /**
     * Compact constructor for defensive programming, input sanitization, and forensic logging.
     * Acts as the final gatekeeper for data integrity, ensuring that transient inputs are
     * sanitized and invalid attempts are logged for security analysis.
     */
    public GenerateHashCommand {
        Objects.requireNonNull(tenantId, "tenantId cannot be null");
        Objects.requireNonNull(payload, "payload cannot be null");
        Objects.requireNonNull(algorithm, "algorithm cannot be null");
        Objects.requireNonNull(sourceService, "sourceService cannot be null");
        Objects.requireNonNull(executor, "executor cannot be null");
        Objects.requireNonNull(asSerialKey, "asSerialKey cannot be null");

        // Sanitization: Normalize whitespace to prevent data contamination
        tenantId = tenantId.trim();
        payload = payload.trim();
        sourceService = sourceService.trim();
        executor = executor.trim();

        // Defense-in-depth: Validation check for non-web instantiation contexts
        if (tenantId.isBlank() || payload.isBlank() || sourceService.isBlank() || executor.isBlank()) {
            log.error("[ACTION: GENERATE_HASH_VALIDATION] - CRITICAL: Pipeline aborted due to malformed command input. [TENANT: {}] [SERVICE: {}] [EXECUTOR: {}]", tenantId, sourceService, executor);
            throw new IllegalArgumentException("Invalid command state: tenantId, payload, sourceService, and executor must not be blank.");
        }
    }
}