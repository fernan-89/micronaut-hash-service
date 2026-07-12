package com.thinklab.infrastructure.adapter.in.web.dto.response;

import com.thinklab.domain.model.HashAudit;
import io.micronaut.core.annotation.Introspected;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.serde.annotation.Serdeable;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.Map;

/**
 * Response DTO: Public-facing projection of a forensic audit log entry.
 * This object represents a single point-in-time event recorded within the system's
 * immutable audit trail. It follows the Projection Pattern to ensure the Domain
 * remains shielded while providing high-quality traceability metadata to the client.
 *
 * <p><b>Architectural Roles:</b></p>
 * <ul>
 *     <li><b>Immutable:</b> Java Record prevents side-effects during reactive streaming.</li>
 *     <li><b>Sanitized:</b> Only exposes business-relevant forensic data.</li>
 *     <li><b>AOT Optimized:</b> Compiled serialization via Micronaut Serde for low-latency.</li>
 * </ul>
 *
 * @param id        The unique system identifier for this audit record.
 * @param txId      The correlation transaction ID used to group related operations.
 * @param tenantId  The isolated tenant context where the operation occurred.
 * @param operation The standardized business operation name (e.g., GENERATION, REVOCATION).
 * @param status    The resulting status of the operation (e.g., SUCCESS, FAILED).
 * @param executorId The identity of the agent (user or service) that authorized the action.
 * @param timestamp The UTC instant when the event was recorded.
 * @param metadata  Contextual key-value pairs containing operation-specific details.
 */
@Serdeable
@Introspected
@Schema(
        name = "HashAuditResponse",
        description = "Standardized forensic metadata representing a specific lifecycle event of a hash token."
)
public record HashAuditResponse(
        @Schema(description = "Internal unique audit ID", example = "64b5f9a2e4b011a2b3c4d5e6")
        String id,

        @Schema(description = "Correlation ID for transaction tracing", example = "TX-88293-AZ-99")
        String txId,

        @Schema(description = "Isolated tenant identifier", example = "THINKLAB-PRD-01")
        String tenantId,

        @Schema(description = "The business operation performed", example = "REVOCATION")
        String operation,

        @Schema(description = "The outcome of the operation", example = "SUCCESS")
        String status,

        @Schema(description = "The agent who authorized the action", example = "security-admin-01")
        String executorId,

        @Schema(description = "Execution timestamp in ISO-8601 format")
        Instant timestamp,

        @Schema(description = "Additional operation-specific contextual metadata")
        Map<String, Object> metadata
) {

    /**
     * Factory method to project a Domain Audit aggregate into a sanitized API response.
     * This transition ensures that the Domain Layer remains agnostic of web formatting.
     *
     * @param domain The pure domain audit aggregate.
     * @return A mapped and serialized-ready response DTO.
     */
    public static HashAuditResponse fromDomain(@NonNull HashAudit domain) {
        return new HashAuditResponse(
                domain.id(),
                domain.txId(),
                domain.tenantId(),
                domain.operation(),
                domain.status(),
                domain.executorId(),
                domain.timestamp(),
                domain.metadata() != null ? Map.copyOf(domain.metadata()) : Map.of()
        );
    }
}