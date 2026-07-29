package com.thinklab.infrastructure.adapter.in.web.dto.response;

import com.thinklab.domain.model.HashToken;
import com.thinklab.domain.valueobject.HashAlgorithm;
import com.thinklab.domain.valueobject.HashStatus;
import io.micronaut.core.annotation.Introspected;
import io.micronaut.serde.annotation.Serdeable;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Infrastructure DTO: Web response projection for a cryptographic {@link HashToken}.
 *
 * <p><b>Architectural Role:</b>
 * This record serves as the primary read-model projection for the external API. It acts as a
 * defensive shield for the domain aggregate, intentionally omitting highly sensitive operational
 * data (such as the raw cryptographic payload and internal executor identities) to ensure
 * external consumers only receive a sanitized, safe view of the registry.
 *
 * <p><b>Contractual Obligations:</b>
 * <ul>
 * <li><b>Projection Pattern:</b> Ensures strict isolation. Changes to the internal structure of
 *     the {@code HashToken} aggregate will not leak to or break the API contract unless explicitly mapped.</li>
 * <li><b>AOT Compilation Strategy:</b> Employs Micronaut's {@code @Serdeable} and {@code @Introspected}
 *     for reflection-free, zero-allocation serialization, ensuring ultra-low latency JSON streaming
 *     within Netty EventLoops.</li>
 * <li><b>Defensive Integrity (Fail-Fast):</b> The compact constructor guarantees that no corrupted,
 *     null, or malformed state can be serialized.</li>
 * </ul>
 *
 * @param id            The universally unique identifier (UUID) derived from the deterministic seed.
 * @param tenantId      The strictly isolated tenant boundary owner.
 * @param sourceService The identifier of the microservice or system that requested the generation.
 * @param generatedHash The final calculated, sanitized cryptographic hash string.
 * @param algorithm     The cryptographic algorithm utilized (e.g., SHA3_512).
 * @param status        The current operational lifecycle status.
 * @param createdAt     The exact UTC instant of the initial record materialization.
 * @param updatedAt     The exact UTC instant of the last lifecycle state change (null if never modified).
 * @param version       The optimistic locking version for database concurrency control.
 *
 * @author ThinkLab
 * @since 1.0
 */
@Serdeable
@Introspected
@Schema(
        name = "HashResponse",
        description = "Standardized, immutable response payload representing a sanitized cryptographic token and its lifecycle metadata."
)
public record HashResponse(

        @Schema(description = "Universally unique identifier for the hash registry.", example = "9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d")
        UUID id,

        @Schema(description = "Isolated tenant identifier boundary.", example = "THINKLAB-PRD-01")
        String tenantId,

        @Schema(description = "Originating microservice or upstream system name.", example = "payment-gateway")
        String sourceService,

        @Schema(description = "The computed cryptographic hash result.", example = "a5e1...f3d9")
        String generatedHash,

        @Schema(description = "The cryptographic algorithm strategy utilized.")
        HashAlgorithm algorithm,

        @Schema(description = "The current operational lifecycle status of the token.")
        HashStatus status,

        @Schema(description = "Generation timestamp in strict UTC ISO-8601 format.")
        Instant createdAt,

        @Schema(description = "Last update timestamp in strict UTC ISO-8601 format (null if unaltered).")
        Instant updatedAt,

        @Schema(description = "Concurrency control version used for optimistic locking.", example = "1")
        Long version
) {

    /**
     * Compact constructor to enforce programmatic fail-fast validation for the projection.
     *
     * <p><b>Contract:</b> Guarantees that the serialization engine will never process an uninitialized
     * or logically corrupted state. The {@code updatedAt} field is uniquely permitted to be null.
     *
     * @throws NullPointerException if any mandatory parameter is null.
     * @throws IllegalArgumentException if any mandatory string parameter is blank.
     */
    public HashResponse {
        Objects.requireNonNull(id, "Projection Invariant Violation: ID cannot be null.");
        Objects.requireNonNull(tenantId, "Projection Invariant Violation: Tenant ID cannot be null.");
        Objects.requireNonNull(sourceService, "Projection Invariant Violation: Source Service cannot be null.");
        Objects.requireNonNull(generatedHash, "Projection Invariant Violation: Generated Hash cannot be null.");
        Objects.requireNonNull(algorithm, "Projection Invariant Violation: Algorithm cannot be null.");
        Objects.requireNonNull(status, "Projection Invariant Violation: Status cannot be null.");
        Objects.requireNonNull(createdAt, "Projection Invariant Violation: CreatedAt timestamp cannot be null.");
        Objects.requireNonNull(version, "Projection Invariant Violation: Version cannot be null.");

        if (tenantId.isBlank()) {
            throw new IllegalArgumentException("Projection Invariant Violation: Tenant ID cannot be blank.");
        }
        if (sourceService.isBlank()) {
            throw new IllegalArgumentException("Projection Invariant Violation: Source Service cannot be blank.");
        }
        if (generatedHash.isBlank()) {
            throw new IllegalArgumentException("Projection Invariant Violation: Generated Hash cannot be blank.");
        }
    }

    /**
     * Factory method to safely project a Domain Aggregate Root into this public API Response.
     *
     * <p><b>Reactive Mapping:</b> Designed to be executed asynchronously within a Project Reactor
     * {@code .map()} operator. It selectively filters out the raw payload and executor tracking
     * data to maintain domain secrecy.
     *
     * @param domain The pure, immutable {@link HashToken} aggregate instance.
     * @return A mapped, sanitized, and AOT serialization-ready {@link HashResponse}.
     * @throws NullPointerException if the provided domain aggregate is null.
     */
    public static HashResponse fromDomain(HashToken domain) {
        Objects.requireNonNull(domain, "Infrastructure constraint violated: Domain aggregate cannot be null for projection.");

        return new HashResponse(
                domain.id(),
                domain.tenantId(),
                domain.sourceService(),
                domain.generatedHash(),
                domain.algorithm(),
                domain.status(),
                domain.createdAt(),
                domain.updatedAt(),
                domain.version()
        );
    }
}