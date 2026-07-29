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
 *
 * <p><b>Architectural Role:</b>
 * This immutable command record serves as the formal request payload for token generation workflows.
 * It enforces strict declarative constraints via Jakarta Bean Validation and defensive runtime sanitization
 * to guarantee data integrity and prevent resource exhaustion (DoS mitigation) before entering the application core.
 *
 * <p><b>Contractual Obligations:</b>
 * <ul>
 * <li><b>Immutability:</b> Implemented as a Java record to guarantee thread-safe propagation across reactive streams.</li>
 * <li><b>OOM Prevention:</b> Enforces strict payload sizing limitations to ensure system stability under heavy load.</li>
 * <li><b>Forensic Accountability:</b> Mandates tenant context, source service telemetry, executor identity,
 *     and algorithm specifications to satisfy compliance requirements.</li>
 * <li><b>Defensive Sanitization:</b> Compact constructor trims string fields and executes fail-fast validation checks
 *     with structured logging hooks.</li>
 * </ul>
 *
 * @param tenantId      The unique identifier of the tenant requesting the generation. Must not be blank.
 * @param payload       The raw data to be hashed (strictly limited to 10,000 characters). Must not be blank.
 * @param algorithm     The cryptographic algorithm chosen for this operation. Must not be null.
 * @param sourceService The name of the microservice or system invoking this action. Must not be blank.
 * @param executor      The user or system account executing the action. Must not be blank.
 * @param asSerialKey   Flag indicating if the output hash should be formatted as a serial key. Must not be null.
 *
 * @author ThinkLab
 * @version 1.0.0
 * @since 1.0
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

        @NotNull(message = "asSerialKey flag is mandatory")
        Boolean asSerialKey
) {

    /**
     * Compact constructor for defensive programming, input sanitization, and structured forensic logging.
     * Acts as the final gatekeeper for data integrity, ensuring inputs are normalized and invalid states
     * are intercepted immediately.
     */
    public GenerateHashCommand {
        Objects.requireNonNull(tenantId, "Application constraint violated: tenantId cannot be null.");
        Objects.requireNonNull(payload, "Application constraint violated: payload cannot be null.");
        Objects.requireNonNull(algorithm, "Application constraint violated: algorithm cannot be null.");
        Objects.requireNonNull(sourceService, "Application constraint violated: sourceService cannot be null.");
        Objects.requireNonNull(executor, "Application constraint violated: executor cannot be null.");
        Objects.requireNonNull(asSerialKey, "Application constraint violated: asSerialKey cannot be null.");

        // Normalization: Trim whitespace on string fields (preserving raw payload integrity)
        tenantId = tenantId.trim();
        sourceService = sourceService.trim();
        executor = executor.trim();

        // Defense-in-depth: Programmatic validation check for non-web or direct instantiation contexts
        if (tenantId.isBlank() || payload.isBlank() || sourceService.isBlank() || executor.isBlank()) {
            log.error("[ACTION: GENERATE_HASH_VALIDATION] [TENANT: {}] [SERVICE: {}] [EXECUTOR: {}] - CRITICAL: Pipeline aborted due to malformed command input.", tenantId, sourceService, executor);
            throw new IllegalArgumentException("Application constraint violated: tenantId, payload, sourceService, and executor must not be blank.");
        }
    }
}