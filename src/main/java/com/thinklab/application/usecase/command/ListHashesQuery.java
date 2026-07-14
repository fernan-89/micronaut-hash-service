package com.thinklab.application.usecase.command;

import com.thinklab.domain.valueobject.HashStatus;
import io.micronaut.core.annotation.Introspected;
import io.micronaut.core.annotation.Nullable;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.extern.slf4j.Slf4j;

/**
 * Application Query: Encapsulates filtering and pagination criteria for {@link com.thinklab.domain.model.HashToken} collections.
 * <p>This immutable query object serves as the formal request structure for read-only listing operations.
 * It enforces data isolation via mandatory tenant scoping, prevents resource exhaustion by
 * capping page sizes, and ensures that all queries are sanitized before reaching the persistence layer.</p>
 *
 * <p><b>Architectural Principles (Mission-Critical Pattern):</b></p>
 * <ul>
 * <li><b>Immutability:</b> Implemented as a Java record to ensure thread-safe, consistent query propagation.</li>
 * <li><b>Data Isolation:</b> Strictly enforces tenant scoping to prevent cross-tenant data leakage.</li>
 * <li><b>Resource Protection:</b> Enforces pagination boundaries (Max 100) to mitigate database load and DoS risks.</li>
 * <li><b>Edge Validation:</b> Combines Jakarta Bean Validation with defensive programming to catch malformed queries at the boundary.</li>
 * </ul>
 *
 * @param tenantId The unique identifier of the tenant (Mandatory for isolation).
 * @param status   Optional filter to retrieve only hashes in a specific lifecycle state.
 * @param page     The page index (Starting from 0).
 * @param size     The number of records per page (Max 100).
 */
@Slf4j
@Introspected
public record ListHashesQuery(
        @NotBlank(message = "Tenant ID is mandatory for security isolation")
        @Pattern(regexp = "^[a-zA-Z0-9-]+$", message = "Tenant ID contains invalid characters")
        String tenantId,

        @Nullable
        HashStatus status,

        @Min(0)
        Integer page,

        @Min(1)
        @Max(100)
        Integer size
) {
    /**
     * Compact constructor for defensive programming, input sanitization, and forensic logging.
     * Acts as the final gatekeeper for query integrity, enforcing default pagination values
     * and logging malformed attempts.
     */
    public ListHashesQuery {
        // Validation & Logging: Tenant isolation is non-negotiable
        if (tenantId == null || tenantId.isBlank()) {
            log.error("[ACTION: LIST_HASHES_VALIDATION] - CRITICAL: Pipeline aborted due to missing tenant context in query.");
            throw new IllegalArgumentException("Tenant ID is mandatory for security isolation");
        }

        // Sanitization: Ensure clean ID for database lookup
        tenantId = tenantId.trim();

        // Default values: Ensure safe pagination defaults
        page = (page == null) ? 0 : page;
        size = (size == null) ? 20 : size;
    }
}