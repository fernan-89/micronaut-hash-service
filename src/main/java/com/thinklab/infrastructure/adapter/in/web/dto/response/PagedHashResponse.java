package com.thinklab.infrastructure.adapter.in.web.dto.response;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.serde.annotation.Serdeable;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * Response DTO: Standardized wrapper for paginated hash registry results.
 * This record follows the Collection Projection pattern to provide a consistent
 * structure for API consumers, including both the payload and the pagination
 * metadata required for advanced UI navigation and server-side cursor management.
 *
 * <p><b>Architectural Roles:</b></p>
 * <ul>
 *     <li><b>Immutable:</b> Java Record structure ensures safe state transfer in reactive pipelines.</li>
 *     <li><b>AOT Ready:</b> Pre-compiled serialization via Micronaut Serde for low-latency delivery.</li>
 *     <li><b>Self-Documented:</b> Extensive OpenAPI metadata for seamless client integration.</li>
 * </ul>
 *
 * @param content       The sanitized list of hash records for the current requested page.
 * @param totalElements The total count of existing hashes matching the filter criteria across all pages.
 * @param page          The current zero-indexed page number.
 * @param size          The total number of items per page (page limit).
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
     * Centralizes the DTO creation to maintain controller readability.
     *
     * @param content       Sanitized list of responses.
     * @param totalElements Total count from the repository.
     * @param page          Requested page index.
     * @param size          Requested page limit.
     * @return A complete, immutable PagedHashResponse instance.
     */
    public static PagedHashResponse of(
            @NonNull List<HashResponse> content,
            long totalElements,
            int page,
            int size) {
        return new PagedHashResponse(List.copyOf(content), totalElements, page, size);
    }
}