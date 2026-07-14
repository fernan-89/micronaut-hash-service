package com.thinklab.infrastructure.adapter.in.web.dto.response;

import com.thinklab.domain.model.HashToken;
import io.micronaut.core.annotation.Introspected;
import io.micronaut.serde.annotation.Serdeable;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.annotation.Nonnull;

import java.time.Instant;
import java.util.UUID;

/**
 * Infrastructure DTO: Response projection for the deactivation of a {@link com.thinklab.domain.model.HashToken}.
 * <p>This DTO acts as the formal public-facing projection, ensuring that the domain model
 * remains shielded from API-specific formatting while providing clear state transition
 * evidence to the consumer.</p>
 *
 * <p><b>Architectural Principles (Mission-Critical Pattern):</b></p>
 * <ul>
 * <li><b>Projection Pattern:</b> Shields domain aggregates from external API consumers.</li>
 * <li><b>Immutability:</b> Implemented as a Java record to ensure thread-safe, consistent data transfer.</li>
 * <li><b>AOT Optimized:</b> Compiled serialization via Micronaut Serde for low-latency delivery.</li>
 * </ul>
 *
 * @param id            The unique system identifier of the deactivated token.
 * @param status        The resulting lifecycle status of the token.
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

        @Schema(description = "The agent who authorized the deactivation", example = "security-officer-42")
        String executor,

        @Schema(description = "The business justification for the operation", example = "Reported compromise of the payload origin")
        String reason,

        @Schema(description = "Execution timestamp in ISO-8601 format")
        Instant deactivatedAt
) {

    /**
     * Factory method to project a Domain Aggregate into a sanitized API response.
     * This transition ensures that the Domain Layer remains agnostic of web formatting.
     *
     * @param domain   The pure domain hash token aggregate after deactivation.
     * @param executor The identity of the agent who authorized the action.
     * @param reason   The business justification provided.
     * @return A mapped and serialized-ready response DTO.
     */
    public static DeactivateHashResponse fromDomain(
            @Nonnull HashToken domain,
            @Nonnull String executor,
            @Nonnull String reason) {

        return new DeactivateHashResponse(
                UUID.fromString(domain.id()),
                domain.status().name(),
                executor,
                reason,
                domain.updatedAt()
        );
    }
}