package com.thinklab.infrastructure.adapter.in.web.dto.response;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.serde.annotation.Serdeable;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Infrastructure DTO: Paginated response projection for {@link HashResponse} collections.
 *
 * <p><b>Architectural Role:</b>
 * This record serves as a standard container wrapper for paginated read-model projections. It acts as an
 * anti-corruption boundary, providing external API consumers with structured navigation metadata
 * (page index, page size, total element counts) without leaking underlying database pagination mechanisms.
 *
 * <p><b>Contractual Obligations:</b>
 * <ul>
 * <li><b>Collection Projection:</b> Shields domain aggregates and pagination mechanics from external API clients.</li>
 * <li><b>AOT Compilation Strategy:</b> Employs Micronaut's {@code @Serdeable} and {@code @Introspected}
 *     for reflection-free, zero-allocation serialization within Netty EventLoops.</li>
 * <li><b>Defensive Integrity (Fail-Fast):</b> The compact constructor guarantees that pagination bounds
 *     are logically sound (page >= 0, size > 0) and nested lists are strictly immutable.</li>
 * </ul>
 *
 * @param content       The strictly sanitized, immutable list of hash records for the current page.
 * @param totalElements The global count of records matching the query criteria.
 * @param page          The current zero-indexed page number (must be >= 0).
 * @param size          The maximum volume of records per page (must be > 0).
 *
 * @author ThinkLab
 * @since 1.0
 */
@Serdeable
@Introspected
@Schema(
        name = "PagedHashResponse",
        description = "Paginated container representing a subset of the cryptographic hash registry with advanced navigation metadata."
)
public record PagedHashResponse(

        @Schema(description = "The list of sanitized hash records for the current page.", requiredMode = Schema.RequiredMode.REQUIRED)
        List<HashResponse> content,

        @Schema(description = "The global total number of items matching the query criteria.", example = "1550", requiredMode = Schema.RequiredMode.REQUIRED)
        long totalElements,

        @Schema(description = "The current page number (zero-indexed).", example = "0", requiredMode = Schema.RequiredMode.REQUIRED)
        int page,

        @Schema(description = "The requested page size limit.", example = "20", requiredMode = Schema.RequiredMode.REQUIRED)
        int size
) {

    /**
     * Compact constructor to enforce programmatic fail-fast validation and absolute immutability.
     *
     * <p><b>Contract:</b> Guarantees that invalid pagination bounds (such as negative page indices or
     * zero/negative sizes) or null collections can never be instantiated or serialized.
     *
     * @throws NullPointerException if the content list is null.
     * @throws IllegalArgumentException if page < 0, size <= 0, or totalElements < 0.
     */
    public PagedHashResponse {
        Objects.requireNonNull(content, "Projection Invariant Violation: Content list cannot be null.");

        if (page < 0) {
            throw new IllegalArgumentException("Projection Invariant Violation: Page index cannot be negative (must be >= 0).");
        }
        if (size <= 0) {
            throw new IllegalArgumentException("Projection Invariant Violation: Page size must be strictly positive (must be > 0).");
        }
        if (totalElements < 0) {
            throw new IllegalArgumentException("Projection Invariant Violation: Total elements count cannot be negative.");
        }

        // Defensively copy the list to ensure absolute immutability before serialization.
        // List.copyOf intrinsically rejects null elements, protecting stream integrity.
        content = content.isEmpty() ? Collections.emptyList() : List.copyOf(content);
    }

    /**
     * Fluent factory method to securely assemble a paginated response wrapper.
     *
     * <p><b>Reactive Synergy:</b> Centralizes pagination container assembly within application services
     * or controller stream mappings to maintain architectural consistency.
     *
     * @param content       Sanitized list of hash responses.
     * @param totalElements Global total element count from the repository.
     * @param page          Requested zero-based page index.
     * @param size          Requested page limit size.
     * @return A complete, immutable {@link PagedHashResponse} instance.
     * @throws NullPointerException if the content list is null.
     * @throws IllegalArgumentException if pagination bounds are invalid.
     */
    public static PagedHashResponse of(
            List<HashResponse> content,
            long totalElements,
            int page,
            int size) {
        return new PagedHashResponse(content, totalElements, page, size);
    }
}