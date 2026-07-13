package com.thinklab.infrastructure.adapter.in.web.dto.request;

import com.thinklab.application.usecase.command.ReactivateHashCommand;
import io.micronaut.core.annotation.Introspected;
import io.micronaut.serde.annotation.Serdeable;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request DTO: Input payload for reactivating an inactive cryptographic token.
 * This object serves as the external contract for reactivation requests, ensuring
 * that mandatory forensic metadata (executor and reason) is provided at the edge
 * to uphold Tier 3 security compliance.
 *
 * <p><b>Architectural Roles:</b></p>
 * <ul>
 *     <li><b>Immutable:</b> Java Record prevents state mutation during reactive flow.</li>
 *     <li><b>AOT Ready:</b> Pre-compiled serialization for high-performance containers.</li>
 *     <li><b>Traceable:</b> Enforces business justifications for lifecycle transitions.</li>
 * </ul>
 *
 * @param executor The user or system account authorizing the reactivation.
 * @param reason   The business justification for restoring the token status (Critical for audit).
 */
@Serdeable
@Introspected
@Schema(
        name = "ReactivateHashRequest",
        description = "Payload required to restore an INACTIVE hash to its operational ACTIVE status."
)
public record ReactivateHashRequest(
        @NotBlank(message = "Executor identification is mandatory")
        @Size(max = 100, message = "Executor identification is too long")
        @Schema(description = "Identification of the agent executing the action", example = "security-officer-01")
        String executor,

        @NotBlank(message = "A business reason for reactivation must be provided")
        @Size(min = 5, max = 500, message = "Reason must be between 5 and 500 characters")
        @Schema(description = "Context or justification for the reactivation", example = "System maintenance completed, restoring primary keys")
        String reason
) {

    /**
     * Maps the web request DTO to the application command.
     * This method facilitates the transition from the Infrastructure layer to the
     * Application layer by combining the body parameters with the resource identifier.
     *
     * @param hashId The unique identifier of the hash extracted from the resource path.
     * @return A validated and sanitized ReactivateHashCommand instance.
     */
    public ReactivateHashCommand toCommand(String hashId) {
        return new ReactivateHashCommand(hashId, this.executor, this.reason);
    }
}

