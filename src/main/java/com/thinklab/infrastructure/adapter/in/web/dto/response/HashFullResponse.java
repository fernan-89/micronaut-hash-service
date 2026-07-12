package com.thinklab.infrastructure.adapter.in.web.dto.response;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.serde.annotation.Serdeable;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * Response DTO: Aggregate view of a Hash and its complete lifecycle audit trail.
 * <p>This object serves as a high-performance projection, grouping the current
 * state of a {@link HashResponse} with its associated {@link HashAuditResponse}
 * historical events. This reduces API round-trips for clients requiring a
 * full forensic view of a hash token.</p>
 *
 * <p><b>Architectural Roles:</b></p>
 * <ul>
 * <li><b>Aggregate Projection:</b> Facilitates "Single Source of Truth" retrieval.</li>
 * <li><b>Immutable:</b> Java Record ensures thread-safety during reactive stream composition.</li>
 * <li><b>Composition:</b> Combines state and audit events without violating
 * the Single Responsibility Principle of internal domain entities.</li>
 * </ul>
 *
 * @param hash      The current state projection of the requested hash.
 * @param auditLogs An ordered list (forensic timeline) of all lifecycle events
 * associated with this hash.
 */
@Serdeable
@Introspected
@Schema(
        name = "HashFullResponse",
        description = "Aggregated projection containing current hash state and associated forensic audit trail."
)
public record HashFullResponse(
        @Schema(description = "The current registry metadata for the requested hash")
        HashResponse hash,

        @Schema(description = "The complete chronological history of lifecycle events")
        List<HashAuditResponse> auditLogs
) {

    /**
     * Factory method to encapsulate the composition of Hash and Audit data.
     * This provides a clean API for the Controller to wrap zipped parallel data streams.
     *
     * @param hash      The mapped hash response DTO.
     * @param auditLogs The list of mapped audit response DTOs.
     * @return A consolidated {@link HashFullResponse} object ready for serialization.
     */
    public static HashFullResponse of(@NonNull HashResponse hash, @NonNull List<HashAuditResponse> auditLogs) {
        return new HashFullResponse(hash, auditLogs);
    }
}