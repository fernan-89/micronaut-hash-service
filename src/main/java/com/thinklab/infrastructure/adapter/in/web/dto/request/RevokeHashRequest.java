package com.thinklab.infrastructure.adapter.in.web.dto.request;

import com.thinklab.application.command.RevokeHashCommand;
import io.micronaut.core.annotation.Introspected;
import io.micronaut.serde.annotation.Serdeable;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request DTO: Input payload for permanent hash revocation.
 * This object represents the web-facing intent to irreversibly revoke a cryptographic token.
 * It enforces the capture of mandatory forensic metadata (executor and business reason)
 * at the system boundary to comply with Tier 3 security standards.
 *
 * <p><b>Architectural Role:</b></p>
 * <ul>
 *     <li><b>Defensive:</b> Validates input syntax before domain entry.</li>
 *     <li><b>Immutable:</b> Prevents state mutation within the reactive flow.</li>
 *     <li><b>AOT Optimized:</b> Compiled serialization via Micronaut Serde.</li>
 * </ul>
 *
 * @param executor The user or system account authorizing this irreversible action.
 * @param reason   The business justification for revocation (Critical for security forensics).
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
     * Maps the web request DTO to the internal application command.
     * This method facilitates the transition from the Infrastructure layer to the
     * Application layer, combining body parameters with the resource identifier.
     *
     * @param hashId The unique identifier of the hash extracted from the HTTP path.
     * @return A validated and sanitized RevokeHashCommand instance.
     */
    public RevokeHashCommand toCommand(String hashId) {
        return new RevokeHashCommand(hashId, this.executor, this.reason);
    }
}