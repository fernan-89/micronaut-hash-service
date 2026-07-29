package com.thinklab.infrastructure.adapter.in.web.dto.request;

import com.thinklab.application.usecase.command.RevokeHashCommand;
import io.micronaut.core.annotation.Introspected;
import io.micronaut.serde.annotation.Serdeable;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Objects;
import java.util.UUID;

/**
 * Infrastructure DTO: Web request payload for the permanent and irreversible revocation of a {@link com.thinklab.domain.model.HashToken}.
 *
 * <p><b>Architectural Role:</b>
 * This Data Transfer Object (DTO) serves as the strict edge-defense mechanism for the HTTP revocation endpoint.
 * As revocation is a terminal, destructive state transition, this payload acts as a Zero Trust gatekeeper,
 * guaranteeing that only syntactically pristine and highly detailed forensic metadata penetrates the Application boundary.
 *
 * <p><b>Contractual Obligations:</b>
 * <ul>
 * <li><b>Protocol Translation:</b> Decouples the volatile external HTTP contracts from the immutable
 *     Application Command structures. Ensures malicious or malformed web payloads are rejected instantly.</li>
 * <li><b>Edge Validation (Fail-Fast):</b> Enforces string boundaries and non-blank constraints synchronously,
 *     yielding a 400 Bad Request before consuming Netty EventLoop cycles. Notice the stricter minimum length
 *     (10 characters) for the revocation reason to prevent trivial justifications.</li>
 * <li><b>Forensic Completeness (Zero Trust):</b> Mandates the collection of critical audit metadata (executor identity
 *     and robust business justification) required for Security Operation Center (SOC) compliance reporting.</li>
 * </ul>
 *
 * @param executor The validated principal identifier of the elevated user or system authorizing this terminal action.
 * @param reason   The comprehensive business justification provided for permanently destroying the token's operational status.
 *
 * @author ThinkLab
 * @since 1.0
 */
@Serdeable
@Introspected
@Schema(
        name = "RevokeHashRequest",
        description = "Mandatory forensic payload required to permanently and irreversibly revoke a cryptographic token."
)
public record RevokeHashRequest(

        @NotBlank(message = "Executor identification is universally mandatory for terminal audit compliance.")
        @Size(max = 100, message = "Executor identification exceeds the maximum permitted length of 100 characters.")
        @Schema(description = "Verified identification of the highly privileged agent executing the destructive action.", example = "security-admin-01")
        String executor,

        @NotBlank(message = "A detailed business reason for revocation is mandatory for forensic traceability.")
        @Size(min = 10, max = 500, message = "The permanent revocation justification must be between 10 and 500 characters to ensure detail.")
        @Schema(description = "Detailed business justification for the permanent revocation.", example = "Compromised payload source detected by automated monitoring system.")
        String reason
) {

    /**
     * Compact constructor to enforce programmatic fail-fast validation.
     *
     * <p><b>Contract:</b> Guarantees that even if this DTO is instantiated programmatically outside of the
     * Micronaut HTTP controller (e.g., via message brokers or unit tests), it is physically impossible
     * to formulate a destructive payload with an invalid state.
     *
     * @throws NullPointerException if any mandatory parameter is null.
     * @throws IllegalArgumentException if any mandatory string parameter is blank.
     */
    public RevokeHashRequest {
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
     * @return A pristine, immutable {@link RevokeHashCommand} ready for Use Case execution.
     * @throws NullPointerException if the injected {@code hashId} is null.
     */
    public RevokeHashCommand toCommand(UUID hashId) {
        Objects.requireNonNull(hashId, "Infrastructure constraint violated: Target Hash UUID must not be null during command translation.");
        return new RevokeHashCommand(hashId, this.executor, this.reason);
    }
}