package com.thinklab.infrastructure.adapter.in.web.dto.request;

import com.thinklab.application.usecase.command.DeactivateHashCommand;
import io.micronaut.core.annotation.Introspected;
import io.micronaut.serde.annotation.Serdeable;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request DTO: Input payload for hash deactivation.
 * This DTO acts as the web-facing representation of the deactivation intent.
 * It enforces strict validation on the executor and reason fields, ensuring
 * high-quality forensic metadata is captured at the edge.
 *
 * <p><b>Architectural Role:</b></p>
 * <ul>
 *     <li><b>Protocol Translation:</b> Maps incoming JSON to the internal Domain Command.</li>
 *     <li><b>Defensive Boundary:</b> Prevents malformed requests from entering the system.</li>
 *     <li><b>AOT Ready:</b> Compiled serialization via Micronaut Serde.</li>
 * </ul>
 *
 * @param executor The user or system account authorizing the deactivation.
 * @param reason   The business justification (Required for compliance/audit).
 */
@Serdeable
@Introspected
@Schema(
        name = "DeactivateHashRequest",
        description = "Payload required to suspend the operational status of a cryptographic token."
)
public record DeactivateHashRequest(
        @NotBlank(message = "Executor identification is mandatory")
        @Size(max = 100, message = "Executor identification is too long")
        @Schema(description = "Identification of the agent executing the action", example = "security-officer-42")
        String executor,

        @NotBlank(message = "A business reason for deactivation must be provided")
        @Size(min = 5, max = 500, message = "Reason must be between 5 and 500 characters")
        @Schema(description = "Business justification for the deactivation", example = "Reported compromise of the payload origin")
        String reason
) {

    /**
     * Maps the web request DTO to the application command.
     * This method facilitates the transition from the Infrastructure layer to the 
     * Application layer by combining the body parameters with the path variable.
     *
     * @param hashId The unique identifier of the hash extracted from the HTTP Path.
     * @return A validated and sanitized DeactivateHashCommand.
     */
    public DeactivateHashCommand toCommand(String hashId) {
        return new DeactivateHashCommand(hashId, this.executor, this.reason);
    }
}