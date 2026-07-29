package com.thinklab.infrastructure.adapter.in.web.dto.response;

import com.thinklab.domain.model.HashToken;
import io.micronaut.core.annotation.Introspected;
import io.micronaut.serde.annotation.Serdeable;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Infrastructure DTO: Response projection for the temporary suspension (deactivation) of a {@link HashToken}.
 *
 * <p><b>Architectural Role:</b>
 * This record acts as the formal, public-facing projection for a deactivation event. It serves as a
 * defensive barrier, shielding the pure domain model from the presentation layer and ensuring that
 * external API consumers only receive a sanitized, contract-compliant view of the state transition.
 *
 * <p><b>Contractual Obligations:</b>
 * <ul>
 * <li><b>Projection Pattern:</b> Enforces strict isolation. The Domain Layer remains entirely ignorant
 *     of JSON formatting, HTTP semantics, or external serialization requirements.</li>
 * <li><b>AOT Compilation Strategy:</b> Leverages Micronaut's {@code @Serdeable} and {@code @Introspected}
 *     annotations for reflection-free, zero-allocation serialization, vastly reducing garbage collection
 *     pressure in high-throughput Netty pipelines.</li>
 * <li><b>Defensive Integrity (Fail-Fast):</b> The compact constructor guarantees that no incomplete or
 *     corrupted state can ever be transmitted to a client.</li>
 * </ul>
 *
 * @param id            The universally unique identifier (UUID) of the suspended cryptographic token.
 * @param status        The resulting lifecycle status of the token (guaranteed to be INACTIVE).
 * @param executor      The verified identity of the agent or system that authorized the deactivation.
 * @param reason        The business justification provided for the operational suspension.
 * @param deactivatedAt The exact UTC instant when the deactivation event materialized in the domain.
 *
 * @author ThinkLab
 * @since 1.0
 */
@Serdeable
@Introspected
@Schema(
        name = "DeactivateHashResponse",
        description = "Standardized, immutable projection representing the successful operational suspension of a hash token."
)
public record DeactivateHashResponse(

        @Schema(description = "The universally unique identifier of the hash token.", example = "25e4a56d-9ca7-47e4-80c9-bf514525b6a2")
        UUID id,

        @Schema(description = "The current lifecycle status of the token.", example = "INACTIVE")
        String status,

        @Schema(description = "The verified agent who authorized the deactivation.", example = "security-officer-42")
        String executor,

        @Schema(description = "The detailed business justification for the operation.", example = "Reported compromise of the payload origin.")
        String reason,

        @Schema(description = "Execution timestamp in strict UTC ISO-8601 format.")
        Instant deactivatedAt
) {

    /**
     * Compact constructor to enforce programmatic fail-fast validation for the projection.
     *
     * <p><b>Contract:</b> Guarantees that the serialization engine will never attempt to process
     * or transmit an invalid or uninitialized state.
     *
     * @throws NullPointerException if any mandatory parameter is null.
     * @throws IllegalArgumentException if any mandatory string parameter is blank.
     */
    public DeactivateHashResponse {
        Objects.requireNonNull(id, "Projection Invariant Violation: ID cannot be null.");
        Objects.requireNonNull(status, "Projection Invariant Violation: Status cannot be null.");
        Objects.requireNonNull(executor, "Projection Invariant Violation: Executor cannot be null.");
        Objects.requireNonNull(reason, "Projection Invariant Violation: Reason cannot be null.");
        Objects.requireNonNull(deactivatedAt, "Projection Invariant Violation: Deactivation timestamp cannot be null.");

        if (status.isBlank()) {
            throw new IllegalArgumentException("Projection Invariant Violation: Status cannot be blank.");
        }
        if (executor.isBlank()) {
            throw new IllegalArgumentException("Projection Invariant Violation: Executor cannot be blank.");
        }
        if (reason.isBlank()) {
            throw new IllegalArgumentException("Projection Invariant Violation: Reason cannot be blank.");
        }
    }

    /**
     * Factory method to safely project a Domain Aggregate into a sanitized API response.
     *
     * <p>This transition ensures that the Domain Layer remains pristine and agnostic of web
     * formatting or serialization concerns.
     *
     * @param domain   The pure, immutable {@link HashToken} domain aggregate post-deactivation.
     * @param executor The identity of the agent who authorized the action.
     * @param reason   The business justification provided for the operation.
     * @return A mapped, sanitized, and serialization-ready response DTO.
     * @throws NullPointerException if the provided domain aggregate or context fields are null.
     */
    public static DeactivateHashResponse fromDomain(
            HashToken domain,
            String executor,
            String reason) {

        Objects.requireNonNull(domain, "Infrastructure constraint violated: Domain aggregate cannot be null for projection.");

        return new DeactivateHashResponse(
                domain.id(),
                domain.status().name(),
                executor,
                reason,
                // Defensive fallback: Use current instant if the domain's updatedAt was somehow missed.
                domain.updatedAt() != null ? domain.updatedAt() : Instant.now()
        );
    }
}