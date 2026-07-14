package com.thinklab.infrastructure.adapter.in.web.dto.request;

import com.thinklab.application.usecase.command.GenerateHashCommand;
import com.thinklab.domain.valueobject.HashAlgorithm;
import io.micronaut.core.annotation.Introspected;
import io.micronaut.serde.annotation.Serdeable;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Infrastructure DTO: Web request payload for the generation of a cryptographic {@link com.thinklab.domain.model.HashToken}.
 * <p>This DTO acts as the formal interface definition for the HTTP generation endpoint.
 * It enforces input validation at the edge, preventing malformed requests and resource
 * exhaustion (DoS mitigation) before entering the Application layer.</p>
 *
 * <p><b>Architectural Principles (Mission-Critical Pattern):</b></p>
 * <ul>
 * <li><b>Protocol Translation:</b> Decouples external API contracts from internal Application Command structures.</li>
 * <li><b>Edge Validation:</b> Enforces schema compliance and business constraints (e.g., payload size limits) before processing.</li>
 * <li><b>Observability-Ready:</b> Captures mandatory context (tenant, source, executor) required for forensic auditability.</li>
 * </ul>
 *
 * @param tenantId      The unique identifier of the tenant requesting the generation.
 * @param payload       The raw data to be hashed.
 * @param algorithm     The cryptographic algorithm chosen for this operation.
 * @param sourceService The name of the microservice or system invoking this action.
 * @param executor      The principal identifier of the user or system authorizing this action.
 * @param asSerialKey   Flag indicating if the output should be formatted as a serial key.
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

        @NotBlank(message = "Payload is mandatory")
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
     * Maps the web request DTO to the domain-compliant application command.
     * This method acts as the translation layer between the Transport Protocol (HTTP)
     * and the Application Use Case boundary.
     *
     * @return A validated and sanitized {@link GenerateHashCommand}.
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