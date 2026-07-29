package com.thinklab.infrastructure.adapter.in.web.dto.request;

import com.thinklab.application.usecase.command.ReactivateHashCommand;
import io.micronaut.core.annotation.Introspected;
import io.micronaut.serde.annotation.Serdeable;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Objects;
import java.util.UUID;

/**
 * Infrastructure DTO: Web request payload for the reactivation of an INACTIVE {@link com.thinklab.domain.model.HashToken}.
 *
 * <p><b>Architectural Role:</b>
 * This Data Transfer Object (DTO) serves as the strict edge-defense mechanism for the HTTP reactivation endpoint.
 * It acts as an Anti-Corruption Layer (ACL), applying rigorous JSR-380 validation to guarantee that only
 * syntactically pristine forensic metadata penetrates the Application boundary.
 *
 * <p><b>Contractual Obligations:</b>
 * <ul>
 * <li><b>Protocol Translation:</b> Decouples the volatile external HTTP contracts from the immutable
 *     Application Command structures, ensuring changes in web payloads do not leak into the Domain.</li>
 * <li><b>Edge Validation (Fail-Fast):</b> Enforces string boundaries and non-blank constraints synchronously,
 *     yielding a 400 Bad Request before utilizing any Netty EventLoop cycles for business processing.</li>
 * <li><b>Forensic Completeness:</b> Mandates the collection of audit metadata (executor identity and business justification)
 *     required to fulfill compliance, observability, and zero-trust logging mandates.</li>
 * </ul>
 *
 * @param executor The validated principal identifier of the user, service account, or system authorizing this action.
 * @param reason   The comprehensive business justification provided for restoring the operational status of the token.
 *
 * @author ThinkLab
 * @since 1.0
 */
@Serdeable
@Introspected
@Schema(
        name = "ReactivateHashRequest",
        description = "Mandatory forensic payload required to restore an INACTIVE hash registry back to its ACTIVE operational status."
)
public record ReactivateHashRequest(

        @NotBlank(message = "Executor identification is universally mandatory for audit compliance.")
        @Size(max = 100, message = "Executor identification exceeds the maximum permitted length of 100 characters.")
        @Schema(description = "Verified identification of the agent executing the restoration action.", example = "security-officer-01")
        String executor,

        @NotBlank(message = "A business reason for reactivation is mandatory for forensic traceability.")
        @Size(min = 5, max = 500, message = "The reactivation justification must be between 5 and 500 characters.")
        @Schema(description = "Detailed business justification for the operational restoration.", example = "System maintenance completed, restoring primary keys.")
        String reason
) {

    /**
     * Compact constructor to enforce programmatic fail-fast validation.
     *
     * <p><b>Contract:</b> Guarantees that even if this DTO is instantiated programmatically outside of the
     * Micronaut HTTP controller (e.g., via message brokers or unit tests), it is physically impossible
     * to create an invalid state.
     *
     * @throws NullPointerException if any mandatory parameter is null.
     * @throws IllegalArgumentException if any mandatory string parameter is blank.
     */
    public ReactivateHashRequest {
        Objects.requireNonNull(executor, "Edge Invariant Violation: Executor cannot be null.");
        Objects.requireNonNull(reason, "Edge Invariant Violation: Reason cannot be null.");

        if (executor.isBlank()) {
            throw new IllegalArgumentException("Edge Invariant Violation: Executor cannot be blank.");
        }
        if (reason.isBlank()) {
            throw new IllegalArgumentException("Edge Invariant Violation: Reason cannot be blank.");
        }
    }

    /**
     * Translates the strictly validated web request payload into a domain-compliant Application Command.
     *
     * <p><b>Contract:</b> This method bridges the HTTP Transport Protocol and the Application Use Case boundary.
     * It defensively guarantees the injection of a valid, BSON-compliant structural identifier.
     *
     * @param hashId The universally unique identifier (UUID) of the target hash, extracted securely from the HTTP Path.
     * @return A pristine, immutable {@link ReactivateHashCommand} ready for Use Case execution.
     * @throws NullPointerException if the injected {@code hashId} is null.
     */
    public ReactivateHashCommand toCommand(UUID hashId) {
        Objects.requireNonNull(hashId, "Infrastructure constraint violated: Target Hash UUID must not be null during command translation.");
        return new ReactivateHashCommand(hashId, this.executor, this.reason);
    }
}