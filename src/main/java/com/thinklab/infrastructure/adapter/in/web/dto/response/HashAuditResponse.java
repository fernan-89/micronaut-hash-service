package com.thinklab.infrastructure.adapter.in.web.dto.response;

import com.thinklab.domain.model.HashAudit;
import io.micronaut.core.annotation.Introspected;
import io.micronaut.serde.annotation.Serdeable;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Infrastructure DTO: Web response projection for the immutable forensic audit trail of a {@link HashAudit}.
 *
 * <p><b>Architectural Role:</b>
 * This record acts as the formal, public-facing API projection for forensic telemetry. It is the external
 * representation of the "Audit" half of the CQRS "360° Full View" pattern, ensuring that pure domain models
 * are never leaked to or tightly coupled with the HTTP transport boundary.
 *
 * <p><b>Contractual Obligations:</b>
 * <ul>
 * <li><b>Projection Pattern:</b> Shields domain aggregates from external API consumers, decoupling
 *     internal forensic storage formats from external JSON schemas.</li>
 * <li><b>AOT Compilation Strategy:</b> Employs Micronaut's {@code @Serdeable} and {@code @Introspected}
 *     for reflection-free, zero-allocation serialization, ensuring ultra-low latency streaming of
 *     audit logs in non-blocking Netty pipelines.</li>
 * <li><b>Defensive Integrity (Fail-Fast):</b> The compact constructor guarantees that no corrupted,
 *     null, or malformed state can be serialized. The metadata map is defensively copied to prevent
 *     serialization mutation bugs.</li>
 * </ul>
 *
 * @param id         The universally unique identifier (UUID) for this specific forensic audit record.
 * @param txId       The reactive correlation transaction UUID used to group cascading operations.
 * @param tenantId   The strictly isolated tenant context where the operation occurred.
 * @param operation  The standardized business operation name (e.g., "CREATION", "REVOCATION").
 * @param status     The resulting lifecycle outcome of the operation (e.g., "SUCCESS").
 * @param executorId The verified identity of the agent or system that authorized the action.
 * @param timestamp  The exact UTC instant when the event materialized in the domain.
 * @param metadata   Rich contextual key-value pairs providing operation-specific telemetry.
 *
 * @author ThinkLab
 * @since 1.0
 */
@Serdeable
@Introspected
@Schema(
        name = "HashAuditResponse",
        description = "Standardized, immutable forensic metadata representing a specific lifecycle event of a cryptographic token."
)
public record HashAuditResponse(

        @Schema(description = "Universally unique identifier for the forensic record.", example = "123e4567-e89b-12d3-a456-426614174000")
        UUID id,

        @Schema(description = "Correlation UUID for reactive transaction tracing.", example = "987e6543-e21b-34d1-b567-426614174999")
        UUID txId,

        @Schema(description = "Isolated tenant boundary identifier.", example = "THINKLAB-PRD-01")
        String tenantId,

        @Schema(description = "The specific business operation executed.", example = "REVOCATION")
        String operation,

        @Schema(description = "The definitive outcome of the operation.", example = "SUCCESS")
        String status,

        @Schema(description = "The verified agent who authorized the action.", example = "security-admin-01")
        String executorId,

        @Schema(description = "Execution timestamp in strict UTC ISO-8601 format.")
        Instant timestamp,

        @Schema(description = "Additional operation-specific contextual telemetry.")
        Map<String, Object> metadata
) {

    /**
     * Compact constructor to enforce programmatic fail-fast validation for the projection.
     *
     * <p><b>Contract:</b> Guarantees that the serialization engine will never attempt to process
     * an uninitialized state. Defensively protects nested collections.
     *
     * @throws NullPointerException if any mandatory parameter is null, or if the metadata map contains null keys/values.
     * @throws IllegalArgumentException if any mandatory string parameter is blank.
     */
    public HashAuditResponse {
        Objects.requireNonNull(id, "Projection Invariant Violation: Audit ID cannot be null.");
        Objects.requireNonNull(txId, "Projection Invariant Violation: Transaction ID cannot be null.");
        Objects.requireNonNull(tenantId, "Projection Invariant Violation: Tenant ID cannot be null.");
        Objects.requireNonNull(operation, "Projection Invariant Violation: Operation cannot be null.");
        Objects.requireNonNull(status, "Projection Invariant Violation: Status cannot be null.");
        Objects.requireNonNull(executorId, "Projection Invariant Violation: Executor ID cannot be null.");
        Objects.requireNonNull(timestamp, "Projection Invariant Violation: Timestamp cannot be null.");

        if (tenantId.isBlank()) {
            throw new IllegalArgumentException("Projection Invariant Violation: Tenant ID cannot be blank.");
        }
        if (operation.isBlank()) {
            throw new IllegalArgumentException("Projection Invariant Violation: Operation cannot be blank.");
        }
        if (status.isBlank()) {
            throw new IllegalArgumentException("Projection Invariant Violation: Status cannot be blank.");
        }
        if (executorId.isBlank()) {
            throw new IllegalArgumentException("Projection Invariant Violation: Executor ID cannot be blank.");
        }

        // Defensively copy the map to ensure absolute immutability before serialization.
        // Map.copyOf intrinsically rejects null keys and null values.
        metadata = (metadata == null || metadata.isEmpty())
                ? Collections.emptyMap()
                : Map.copyOf(metadata);
    }

    /**
     * Factory method to safely project a Domain Audit aggregate into a sanitized API response.
     *
     * <p>This transition ensures that the Domain Layer remains pristine and completely isolated
     * from HTTP formatting and infrastructure constraints.
     *
     * @param domain The pure, immutable {@link HashAudit} domain aggregate.
     * @return A mapped, sanitized, and AOT serialization-ready response DTO.
     * @throws NullPointerException if the provided domain aggregate is null.
     */
    public static HashAuditResponse fromDomain(HashAudit domain) {
        Objects.requireNonNull(domain, "Infrastructure constraint violated: Domain aggregate cannot be null for projection.");

        return new HashAuditResponse(
                domain.id(),
                domain.txId(),
                domain.tenantId(),
                domain.operation(),
                domain.status(),
                domain.executorId(),
                domain.timestamp(),
                domain.metadata()
        );
    }
}