package com.thinklab.infrastructure.adapter.in.web.dto.response;

import com.thinklab.domain.model.HashToken;
import io.micronaut.core.annotation.Introspected;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.serde.annotation.Serdeable;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO: Public-facing projection of a deactivated hash token.
 * This object represents the state of a hash token immediately following a successful
 * deactivation command. It follows the Projection Pattern to ensure the Domain
 * remains shielded while providing clear audit and state metadata to the client.
 *
 * <p><b>Architectural Roles:</b></p>
 * <ul>
 *     <li><b>Immutable:</b> Java Record prevents side-effects during reactive streaming.</li>
 *     <li><b>Sanitized:</b> Only exposes business-relevant state transition data.</li>
 *     <li><b>AOT Optimized:</b> Compiled serialization via Micronaut Serde for low-latency.</li>
 * </ul>
 *
 * @param id            The unique system identifier of the deactivated token.
 * @param status        The resulting lifecycle status (e.g., INACTIVE, REVOKED).
 * @param executor      The identity of the agent who authorized the deactivation.
 * @param reason        The business justification provided for the operation.
 * @param deactivatedAt The UTC instant when the deactivation event was committed.
 */
@Serdeable
@Introspected
@Schema(
        name = "DeactivateHashResponse",
        description = "Standardized response representing the successful deactivation of a hash token."
)
public record DeactivateHashResponse(

        @Schema(description = "The unique identifier of the hash token", example = "25e4a56d-9ca7-47e4-80c9-bf514525b6a2")
        UUID id,

        @Schema(description = "The current lifecycle status of the token", example = "INACTIVE")
        String status,

        @Schema(description = "The agent who authorized the deactivation", example = "admin-user")
        String executor,

        @Schema(description = "The business justification for the operation", example = "Manutenção programada")
        String reason,

        @Schema(description = "Execution timestamp in ISO-8601 format")
        Instant deactivatedAt
) {

    /**
     * Factory method to project a Domain Aggregate into a sanitized API response.
     * This transition ensures that the Domain Layer remains agnostic of web formatting.
     *
     * @param domain   The pure domain hash token aggregate after deactivation.
     * @param executor The identity of the agent who authorized the action (from request).
     * @param reason   The business justification (from request).
     * @return A mapped and serialized-ready response DTO.
     */
    public static DeactivateHashResponse fromDomain(
            @NonNull HashToken domain,
            String executor,
            String reason) {

        return new DeactivateHashResponse(
                UUID.fromString(domain.id()),
                domain.status().name(),
                executor, // Injetado via parâmetro
                reason,   // Injetado via parâmetro
                domain.updatedAt()
        );
    }
}