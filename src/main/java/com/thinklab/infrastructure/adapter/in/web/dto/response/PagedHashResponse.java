package com.thinklab.infrastructure.adapter.in.web.dto.response;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.serde.annotation.Serdeable;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.annotation.Nonnull;

import java.util.List;

/**
 * Infrastructure DTO: Paginated response projection for {@link HashResponse} collections.
 * <p>This DTO acts as the formal interface definition for paginated retrieval endpoints.
 * It provides a standardized structure for API consumers, including metadata required
 * for advanced UI navigation and server-side management.</p>
 *
 * <p><b>Architectural Principles (Mission-Critical Pattern):</b></p>
 * <ul>
 * <li><b>Collection Projection:</b> Shields domain aggregates from external API consumers while providing pagination context.</li>
 * <li><b>Immutability:</b> Implemented as a Java record to ensure thread-safe, consistent data transfer.</li>
 * <li><b>AOT Optimized:</b> Compiled serialization via Micronaut Serde for low-latency delivery.</li>
 * </ul>
 *
 * @param content       The sanitized list of hash records for the current requested page.
 * @param totalElements The total count of existing hashes matching the filter criteria.
 * @param page          The current zero-indexed page number.
 * @param size          The total number of items per page.
 */
@Serdeable
@Introspected
@Schema(
        name = "PagedHashResponse",
        description = "Paginated container representing a subset of the cryptographic hash registry with metadata."
)
public record PagedHashResponse(
        @Schema(description = "The list of hash records for the current page")
        List<HashResponse> content,

        @Schema(description = "The global total number of items matching the query", example = "1550")
        long totalElements,

        @Schema(description = "The current page number (0-indexed)", example = "0")
        int page,

        @Schema(description = "The requested page size", example = "20")
        int size
) {

    /**
     * Fluent factory method to assemble a paginated response wrapper.
     * Centralizes DTO creation to maintain controller readability and consistency.
     *
     * @param content       Sanitized list of responses.
     * @param totalElements Total count from the repository.
     * @param page          Requested page index.
     * @param size          Requested page limit.
     * @return A complete, immutable {@link PagedHashResponse} instance.
     */
    public static PagedHashResponse of(
            @Nonnull List<HashResponse> content,
            long totalElements,
            int page,
            int size) {
        return new PagedHashResponse(List.copyOf(content), totalElements, page, size);
    }
}