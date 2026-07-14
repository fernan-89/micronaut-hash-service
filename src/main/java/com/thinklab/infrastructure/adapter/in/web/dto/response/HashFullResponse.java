package com.thinklab.infrastructure.adapter.in.web.dto.response;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.serde.annotation.Serdeable;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.annotation.Nonnull;

import java.util.List;

/**
 * Infrastructure DTO: Aggregated projection containing the current state and forensic history of a {@link com.thinklab.domain.model.HashToken}.
 * <p>This DTO acts as the formal interface definition for the HTTP full retrieval endpoint.
 * It provides a high-performance projection, grouping the current registry state with its
 * associated forensic audit trail to reduce API round-trips for the consumer.</p>
 *
 * <p><b>Architectural Principles (Mission-Critical Pattern):</b></p>
 * <ul>
 * <li><b>Aggregate Projection:</b> Facilitates "Single Source of Truth" retrieval by combining state and history.</li>
 * <li><b>Immutability:</b> Implemented as a Java record to ensure thread-safe, consistent data transfer.</li>
 * <li><b>AOT Optimized:</b> Compiled serialization via Micronaut Serde for low-latency delivery.</li>
 * </ul>
 *
 * @param hash      The current state projection of the requested hash.
 * @param auditLogs The complete chronological history of lifecycle events associated with this hash.
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
     *
     * @param hash      The mapped hash response DTO.
     * @param auditLogs The list of mapped audit response DTOs.
     * @return A consolidated {@link HashFullResponse} object ready for serialization.
     */
    public static HashFullResponse of(@Nonnull HashResponse hash, @Nonnull List<HashAuditResponse> auditLogs) {
        return new HashFullResponse(hash, auditLogs);
    }
}