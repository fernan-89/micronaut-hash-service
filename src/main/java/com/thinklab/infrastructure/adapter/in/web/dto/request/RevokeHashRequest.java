package com.thinklab.infrastructure.adapter.in.web.dto.request;

import com.thinklab.application.usecase.command.RevokeHashCommand;
import io.micronaut.core.annotation.Introspected;
import io.micronaut.serde.annotation.Serdeable;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Infrastructure DTO: Web request payload for the permanent revocation of a {@link com.thinklab.domain.model.HashToken}.
 * <p>This DTO acts as the formal interface definition for the HTTP revocation endpoint.
 * It enforces input validation at the edge, ensuring that only syntactically correct
 * forensic metadata enters the Application layer.</p>
 *
 * <p><b>Architectural Principles (Mission-Critical Pattern):</b></p>
 * <ul>
 * <li><b>Protocol Translation:</b> Decouples external API contracts from internal Application Command structures.</li>
 * <li><b>Edge Validation:</b> Enforces schema compliance and business constraints before processing.</li>
 * <li><b>Observability-Ready:</b> Carries mandatory audit metadata (executor/reason) required for compliance reporting.</li>
 * </ul>
 *
 * @param executor The principal identifier of the user or system authorizing this action.
 * @param reason   The business justification provided for the permanent revocation.
 */
@Serdeable
@Introspected
@Schema(
        name = "RevokeHashRequest",
        description = "Payload required to permanently and irreversibly revoke a cryptographic token."
)
public record RevokeHashRequest(
        @NotBlank(message = "Executor identification is mandatory")
        @Size(max = 100, message = "Executor identification is too long")
        @Schema(description = "Identification of the agent executing the action", example = "security-admin-01")
        String executor,

        @NotBlank(message = "A business reason for revocation must be provided")
        @Size(min = 10, max = 500, message = "Reason must be between 10 and 500 characters")
        @Schema(description = "Context or justification for the permanent revocation", example = "Compromised payload source detected by monitoring system")
        String reason
) {

    /**
     * Maps the web request DTO to the domain-compliant application command.
     * This method acts as the translation layer between the Transport Protocol (HTTP)
     * and the Application Use Case boundary.
     *
     * @param hashId The unique identifier of the hash extracted from the HTTP Path.
     * @return A validated and sanitized {@link RevokeHashCommand}.
     */
    public RevokeHashCommand toCommand(String hashId) {
        return new RevokeHashCommand(hashId, this.executor, this.reason);
    }
}