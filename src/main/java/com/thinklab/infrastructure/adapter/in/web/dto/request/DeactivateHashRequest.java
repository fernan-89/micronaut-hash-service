package com.thinklab.infrastructure.adapter.in.web.dto.request;

import com.thinklab.application.usecase.command.DeactivateHashCommand;
import io.micronaut.core.annotation.Introspected;
import io.micronaut.serde.annotation.Serdeable;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Objects;
import java.util.UUID;

/**
 * Infrastructure DTO: Web request payload for the temporary suspension of a {@link com.thinklab.domain.model.HashToken}.
 *
 * <p><b>Architectural Role:</b>
 * This Data Transfer Object (DTO) serves as the strict edge-defense mechanism for the HTTP deactivation endpoint.
 * It intercepts raw incoming JSON payloads, applying rigorous JSR-380 validation to guarantee that only
 * syntactically pristine forensic metadata penetrates the Application boundary.
 *
 * <p><b>Contractual Obligations:</b>
 * <ul>
 * <li><b>Protocol Translation:</b> Acts as an anti-corruption layer, decoupling the volatile external HTTP
 *     contracts from the immutable Application Command structures.</li>
 * <li><b>Edge Validation (Fail-Fast):</b> Enforces string boundaries and non-blank constraints synchronously,
 *     yielding a 400 Bad Request before utilizing any Netty EventLoop cycles for business processing.</li>
 * <li><b>Forensic Completeness:</b> Mandates the collection of audit metadata (executor identity and business justification)
 *     required to fulfill compliance and observability mandates.</li>
 * </ul>
 *
 * @param executor The validated principal identifier of the user, service account, or system authorizing this action.
 * @param reason   The comprehensive business justification provided for suspending the cryptographic token.
 *
 * @author ThinkLab
 * @since 1.0
 */
@Serdeable
@Introspected
@Schema(
        name = "DeactivateHashRequest",
        description = "Mandatory forensic payload required to suspend the operational status of a cryptographic token."
)
public record DeactivateHashRequest(

        @NotBlank(message = "Executor identification is universally mandatory for audit compliance.")
        @Size(max = 100, message = "Executor identification exceeds the maximum permitted length of 100 characters.")
        @Schema(description = "Verified identification of the agent executing the action.", example = "security-officer-42")
        String executor,

        @NotBlank(message = "A business reason for deactivation is mandatory for forensic traceability.")
        @Size(min = 5, max = 500, message = "The deactivation justification must be between 5 and 500 characters.")
        @Schema(description = "Detailed business justification for the token suspension.", example = "Reported compromise of the payload origin system.")
        String reason
) {

    /**
     * Translates the web request payload into a domain-compliant Application Command.
     *
     * <p><b>Contract:</b> This method bridges the HTTP Transport Protocol and the Application Use Case boundary.
     * It defensively guarantees the injection of a valid structural identifier.
     *
     * @param hashId The universally unique identifier (UUID) of the target hash, extracted securely from the HTTP Path.
     * @return A pristine, immutable {@link DeactivateHashCommand} ready for Use Case execution.
     * @throws NullPointerException if the injected {@code hashId} is null.
     */
    public DeactivateHashCommand toCommand(UUID hashId) {
        Objects.requireNonNull(hashId, "Infrastructure constraint violated: Target Hash UUID must not be null during command translation.");
        return new DeactivateHashCommand(hashId, this.executor, this.reason);
    }
}