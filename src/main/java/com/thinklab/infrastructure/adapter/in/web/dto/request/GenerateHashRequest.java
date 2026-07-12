package com.thinklab.infrastructure.adapter.in.web.dto.request;

import com.thinklab.application.command.GenerateHashCommand;
import com.thinklab.domain.valueobject.HashAlgorithm;
import io.micronaut.core.annotation.Introspected;
import io.micronaut.serde.annotation.Serdeable;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Request DTO: Input payload for cryptographic hash and serial key generation.
 * This object acts as the entry boundary for the API, enforcing strict syntactic
 * validation and providing metadata for automated OpenAPI documentation.
 *
 * <p><b>Architectural Roles:</b></p>
 * <ul>
 *     <li><b>Immutable:</b> Prevents payload tampering during the reactive pipeline.</li>
 *     <li><b>AOT Ready:</b> Uses Micronaut Serde for reflection-free serialization.</li>
 *     <li><b>Defensive:</b> Protects the system against OOM attacks via size limits.</li>
 * </ul>
 *
 * @param tenantId      The unique identifier for the isolated tenant.
 * @param payload       The raw data to be hashed (Max 10,000 characters).
 * @param algorithm     The chosen cryptographic algorithm (SHA-256, BLAKE3, etc.).
 * @param sourceService The identifier of the requesting microservice.
 * @param executor      The identity of the agent authorizing the generation.
 * @param asSerialKey   Whether the result should be formatted as a serial key.
 */
@Serdeable
@Introspected
@Schema(
        name = "GenerateHashRequest",
        description = "Payload required to initiate a new cryptographic token generation process."
)
public record GenerateHashRequest(
        @NotBlank(message = "Tenant ID is mandatory")
        @Schema(description = "Isolated tenant identifier", example = "THINKLAB-PROD-01")
        String tenantId,

        @NotBlank(message = "Payload cannot be empty")
        @Size(max = 10000, message = "Payload exceeds security limits (10k chars)")
        @Schema(description = "Raw content to be hashed", example = "raw-transaction-data-v1")
        String payload,

        @NotNull(message = "Cryptographic algorithm is mandatory")
        @Schema(description = "The algorithm to be used for calculation", example = "SHA_256")
        HashAlgorithm algorithm,

        @NotBlank(message = "Source service identifier is mandatory")
        @Schema(description = "ID of the system requesting the hash", example = "order-management-service")
        String sourceService,

        @NotBlank(message = "Executor identification is mandatory for auditing")
        @Schema(description = "Identification of the agent executing the action", example = "admin-user-01")
        String executor,

        @Schema(description = "If true, formats the output as a serial key", defaultValue = "false")
        boolean asSerialKey
) {

    /**
     * Maps the incoming Web DTO to the internal Application Command.
     * This transition ensures that the Infrastructure layer remains decoupled from
     * the Application layer, applying sanitization during the conversion.
     *
     * @return A sanitized and validated GenerateHashCommand instance.
     */
    public GenerateHashCommand toCommand() {
        return new GenerateHashCommand(
                this.tenantId,
                this.payload,
                this.algorithm,
                this.sourceService,
                this.executor,
                this.asSerialKey
        );
    }
}