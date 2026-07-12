package com.thinklab.infrastructure.adapter.in.web.dto.response;

import com.thinklab.domain.model.HashToken;
import com.thinklab.domain.valueobject.HashAlgorithm;
import com.thinklab.domain.valueobject.HashStatus;
import io.micronaut.core.annotation.Introspected;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.serde.annotation.Serdeable;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

/**
 * Response DTO: Public-facing projection of a cryptographic hash registry.
 * This record follows the Projection Pattern to transform the internal Domain Aggregate
 * into a sanitized, serialized representation suitable for API consumers.
 *
 * <p><b>Architectural Roles:</b></p>
 * <ul>
 *     <li><b>Immutable:</b> Java Record structure prevents state mutation during streaming.</li>
 *     <li><b>Sanitized:</b> Only exposes operational metadata, shielding domain internals.</li>
 *     <li><b>AOT Optimized:</b> Compiled serialization via Micronaut Serde for low footprint.</li>
 * </ul>
 *
 * @param id             Unique system identifier for the hash registry.
 * @param tenantId       The isolated tenant context owner.
 * @param sourceService  Identifier of the microservice that requested the generation.
 * @param generatedHash  The final calculated cryptographic hash string.
 * @param algorithm      The cryptographic standard used (e.g., SHA-256, BLAKE3).
 * @param status         The current lifecycle status (ACTIVE, INACTIVE, REVOKED).
 * @param createdAt      UTC timestamp of the initial generation.
 * @param updatedAt      UTC timestamp of the last status or metadata change.
 * @param version        Concurrency control version (Optimistic Locking).
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
     * @return A mapped and sanitized HashResponse DTO.
     */
    public static HashResponse fromDomain(@NonNull HashToken domain) {
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