package com.thinklab.application.usecase.command;

import com.thinklab.domain.valueobject.HashStatus;
import io.micronaut.core.annotation.Introspected;
import io.micronaut.core.annotation.Nullable;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.extern.slf4j.Slf4j;

import java.util.Objects;

/**
 * Application Query: Encapsulates filtering and pagination criteria for {@link com.thinklab.domain.model.HashToken} collections.
 *
 * <p><b>Architectural Role:</b>
 * This immutable query record serves as the formal request payload for read-only collection listing operations.
 * Adhering strictly to CQRS principles and multi-tenant security requirements, it enforces mandatory tenant
 * isolation, caps page sizes to prevent resource exhaustion (DoS mitigation), and sanitizes inputs at the edge.
 *
 * <p><b>Contractual Obligations:</b>
 * <ul>
 * <li><b>Immutability:</b> Implemented as a Java record to guarantee thread-safe query propagation across reactive streams.</li>
 * <li><b>Multi-Tenant Isolation:</b> Strictly mandates a valid tenant identifier to prevent cross-tenant data leakage.</li>
 * <li><b>Resource Protection:</b> Enforces strict pagination boundaries (Max 100 records per page) to safeguard database performance.</li>
 * <li><b>Defensive Sanitization:</b> Compact constructor normalizes strings, assigns safe fallback pagination defaults,
 *     and executes fail-fast validation checks with structured logging hooks.</li>
 * </ul>
 *
 * @param tenantId The unique identifier of the tenant requesting the list. Must not be blank.
 * @param status   Optional lifecycle status filter to narrow the retrieved collection.
 * @param page     The zero-indexed page number. Defaults to 0 if null.
 * @param size     The number of items per page (bounded between 1 and 100). Defaults to 20 if null.
 *
 * @author ThinkLab
 * @version 1.0.0
 * @since 1.0
 */
@Slf4j
@Introspected
public record ListHashesQuery(
        @NotBlank(message = "Tenant ID is mandatory for security isolation")
        @Pattern(regexp = "^[a-zA-Z0-9-]+$", message = "Tenant ID contains invalid characters")
        String tenantId,

        @Nullable
        HashStatus status,

        @Min(value = 0, message = "Page index must be greater than or equal to 0")
        Integer page,

        @Min(value = 1, message = "Page size must be at least 1")
        @Max(value = 100, message = "Page size must not exceed 100 to prevent resource exhaustion")
        Integer size
) {

    /**
     * Compact constructor for defensive programming, input sanitization, and structured forensic logging.
     * Acts as the final gatekeeper for query integrity, enforcing safe default values and intercepting
     * missing tenant contexts immediately.
     */
    public ListHashesQuery {
        Objects.requireNonNull(tenantId, "Application constraint violated: tenantId cannot be null.");

        // Normalization: Trim whitespace on tenant identifier
        tenantId = tenantId.trim();

        // Defense-in-depth: Programmatic validation check for non-web or direct instantiation contexts
        if (tenantId.isBlank()) {
            log.error("[ACTION: LIST_HASHES_VALIDATION] - CRITICAL: Pipeline aborted due to missing tenant context in query.");
            throw new IllegalArgumentException("Application constraint violated: Tenant ID is mandatory for security isolation.");
        }

        // Apply safe pagination defaults if null
        page = (page == null) ? 0 : page;
        size = (size == null) ? 20 : size;
    }
}