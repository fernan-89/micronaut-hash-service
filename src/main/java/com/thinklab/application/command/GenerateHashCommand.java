package com.thinklab.application.command;

import com.thinklab.domain.valueobject.HashAlgorithm;
import io.micronaut.core.annotation.Introspected;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.Objects;

/**
 * Input Boundary: Command object representing the intent to generate a new cryptographic hash.
 * This record enforces strict input validation using Jakarta Bean Validation and functional
 * sanitization. It acts as a defensive shield against malformed payloads and potential
 * Denial of Service (DoS) attacks via memory exhaustion.
 *
 * <p><b>Architectural Rules:</b></p>
 * <ul>
 *     <li><b>OOM Prevention:</b> Restricts the input payload to 10,000 characters to ensure
 *         system stability during high-concurrency reactive processing.</li>
 *     <li><b>Sanitization:</b> Automatically trims all string inputs to prevent lookup
 *         failures caused by accidental white spaces.</li>
 *     <li><b>Immutable:</b> Guarantees thread-safety throughout the reactive pipeline.</li>
 * </ul>
 *
 * @param tenantId      The unique identifier of the tenant requesting the generation.
 * @param payload       The raw data to be hashed (Limited to 10,000 chars for security).
 * @param algorithm     The cryptographic algorithm chosen for this operation.
 * @param sourceService The name of the microservice or system invoking this action.
 * @param executor      The user or system account executing the action (for audit purposes).
 * @param asSerialKey   Flag indicating if the output should be formatted as a serial key.
 */
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
     * Compact constructor for defensive programming and input sanitization.
     */
    public GenerateHashCommand {
        Objects.requireNonNull(tenantId, "tenantId cannot be null");
        Objects.requireNonNull(payload, "payload cannot be null");
        Objects.requireNonNull(algorithm, "algorithm cannot be null");
        Objects.requireNonNull(sourceService, "sourceService cannot be null");
        Objects.requireNonNull(executor, "executor cannot be null");
        Objects.requireNonNull(asSerialKey, "asSerialKey cannot be null");

        // Sanitization: Ensure clean inputs for business logic and auditing
        tenantId = tenantId.trim();
        payload = payload.trim();
        sourceService = sourceService.trim();
        executor = executor.trim();
    }
}