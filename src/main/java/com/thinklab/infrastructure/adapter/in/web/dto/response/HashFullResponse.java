package com.thinklab.infrastructure.adapter.in.web.dto.response;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.serde.annotation.Serdeable;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Infrastructure DTO: Aggregated projection containing the current state and exhaustive forensic history of a cryptographic token.
 *
 * <p><b>Architectural Role (CQRS 360° Full View - ADR-003/OBS-002):</b>
 * This DTO acts as the formal public-facing projection for the HTTP full-retrieval endpoint.
 * It materializes the "360° Full View" pattern by consolidating the current state of a {@link com.thinklab.domain.model.HashToken}
 * and its complete {@link com.thinklab.domain.model.HashAudit} trail into a single, cohesive payload.
 * This design optimally leverages Project Reactor's {@code Mono.zip()} operator to fetch data in parallel,
 * slashing API round-trips and network latency for the consumer.
 *
 * <p><b>Contractual Obligations:</b>
 * <ul>
 * <li><b>Aggregate Projection:</b> Facilitates a "Single Source of Truth" delivery, strictly isolating
 *     the underlying persistence models from the HTTP transport format.</li>
 * <li><b>Defensive Immutability:</b> Implemented as a strictly immutable Java record. Nested collections
 *     are defensively copied to prevent mutation during asynchronous Netty serialization.</li>
 * <li><b>AOT Compilation Strategy:</b> Leverages Micronaut's {@code @Serdeable} and {@code @Introspected}
 *     for zero-reflection, high-speed JSON encoding, eliminating garbage collection overhead during peak loads.</li>
 * </ul>
 *
 * @param hash      The pristine, sanitized projection of the requested hash's current operational state.
 * @param auditLogs The complete, chronologically ordered history of forensic lifecycle events.
 *
 * @author ThinkLab
 * @since 1.0
 */
@Serdeable
@Introspected
@Schema(
        name = "HashFullResponse",
        description = "Aggregated projection materializing the CQRS 360° Full View, combining current state with immutable forensic history."
)
public record HashFullResponse(

        @Schema(description = "The current registry metadata and operational state for the requested hash.", requiredMode = Schema.RequiredMode.REQUIRED)
        HashResponse hash,

        @Schema(description = "The complete chronological history of immutable lifecycle events.", requiredMode = Schema.RequiredMode.REQUIRED)
        List<HashAuditResponse> auditLogs
) {

    /**
     * Compact constructor to enforce programmatic fail-fast validation for the composite projection.
     *
     * <p><b>Contract:</b> Guarantees that the serialization engine will never process an uninitialized state.
     * Defensively locks the forensic history list to prevent downstream mutations.
     *
     * @throws NullPointerException if the primary hash projection is null, or if the audit list contains null elements.
     */
    public HashFullResponse {
        Objects.requireNonNull(hash, "Projection Invariant Violation: Core HashResponse cannot be null.");

        // Defensively copy the list to ensure absolute immutability before Netty serialization.
        // List.copyOf intrinsically rejects null elements, securing the audit trail integrity.
        auditLogs = (auditLogs == null || auditLogs.isEmpty())
                ? Collections.emptyList()
                : List.copyOf(auditLogs);
    }

    /**
     * Factory method to encapsulate the composition of the Hash and Audit data streams.
     *
     * <p><b>Reactive Synergy:</b> This method is typically invoked as a combinator within a
     * {@code Mono.zip(hashMono, auditListMono, HashFullResponse::of)} pipeline, elegantly merging
     * the parallel asynchronous streams.
     *
     * @param hash      The sanitized hash response DTO.
     * @param auditLogs The projected list of forensic audit response DTOs.
     * @return A consolidated, immutable {@link HashFullResponse} object ready for instantaneous serialization.
     * @throws NullPointerException if the primary hash projection is null.
     */
    public static HashFullResponse of(HashResponse hash, List<HashAuditResponse> auditLogs) {
        return new HashFullResponse(hash, auditLogs);
    }
}