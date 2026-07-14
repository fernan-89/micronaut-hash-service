package com.thinklab.infrastructure.adapter.in.web.dto.response;

import com.thinklab.domain.model.HashToken;
import com.thinklab.domain.valueobject.HashAlgorithm;
import com.thinklab.domain.valueobject.HashStatus;
import io.micronaut.core.annotation.Introspected;
import io.micronaut.serde.annotation.Serdeable;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.time.Instant;

/**
 * Infrastructure DTO: Web response projection for a cryptographic {@link com.thinklab.domain.model.HashToken}.
 * <p>This DTO acts as the formal public-facing projection, ensuring that the domain model
 * remains shielded from API-specific formatting while providing clear state
 * metadata to the consumer.</p>
 *
 * <p><b>Architectural Principles (Mission-Critical Pattern):</b></p>
 * <ul>
 * <li><b>Projection Pattern:</b> Shields domain aggregates from external API consumers.</li>
 * <li><b>Immutability:</b> Implemented as a Java record to ensure thread-safe, consistent data transfer.</li>
 * <li><b>AOT Optimized:</b> Compiled serialization via Micronaut Serde for low-latency delivery.</li>
 * </ul>
 *
 * @param id            Unique system identifier for the hash registry.
 * @param tenantId      The isolated tenant context owner.
 * @param sourceService Identifier of the microservice that requested the generation.
 * @param generatedHash The final calculated cryptographic hash string.
 * @param algorithm     The cryptographic standard used.
 * @param status        The current lifecycle status.
 * @param createdAt     UTC timestamp of the initial generation.
 * @param updatedAt     UTC timestamp of the last status or metadata change.
 * @param version       Concurrency control version (Optimistic Locking).
 */
@Serdeable
@Introspected
@Schema(
        name = "HashResponse",
        description = "Standardized response payload representing a cryptographic token and its lifecycle metadata."
)
public record HashResponse(
        @Schema(description = "Internal unique ID", example = "64b5f9a2e4b011a2b3c4d5e6")
        String id,

        @Schema(description = "Isolated tenant identifier", example = "THINKLAB-PRD-01")
        String tenantId,

        @Schema(description = "Originating microservice name", example = "payment-gateway")
        String sourceService,

        @Schema(description = "The computed cryptographic hash result", example = "a5e1...f3d9")
        String generatedHash,

        @Schema(description = "Cryptographic algorithm used")
        HashAlgorithm algorithm,

        @Schema(description = "Current lifecycle status of the token")
        HashStatus status,

        @Schema(description = "Generation timestamp in ISO-8601 format")
        Instant createdAt,

        @Nullable
        @Schema(description = "Last update timestamp in ISO-8601 format")
        Instant updatedAt,

        @Schema(description = "Concurrency control version", example = "1")
        Long version
) {

    /**
     * Factory method to project a Domain Aggregate Root into this public API Response.
     * This transition ensures that infrastructure-level changes do not leak into the Domain.
     *
     * @param domain The pure domain aggregate instance.
     * @return A mapped and sanitized {@link HashResponse}.
     */
    public static HashResponse fromDomain(@Nonnull HashToken domain) {
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