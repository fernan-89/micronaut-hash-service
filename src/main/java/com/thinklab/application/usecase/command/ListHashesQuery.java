package com.thinklab.application.usecase.command;

import com.thinklab.domain.valueobject.HashStatus;
import io.micronaut.core.annotation.Introspected;
import io.micronaut.core.annotation.Nullable;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import java.util.Objects;

/**
 * Input Boundary: Query object for listing cryptographic hashes with pagination and filters.
 * This record ensures that list requests are sanitized and limited at the boundary to
 * prevent excessive resource consumption and database overhead.
 *
 * <p><b>Invariants:</b></p>
 * <ul>
 *     <li>TenantId is mandatory for data isolation.</li>
 *     <li>Pagination limits are enforced (max 100 items per page).</li>
 *     <li>Status filter is optional but type-safe.</li>
 * </ul>
 *
 * @param tenantId The unique identifier for the tenant (Mandatory for isolation).
 * @param status   Optional filter to retrieve only hashes in a specific state.
 * @param page     The page index (Starting from 0).
 * @param size     The number of records per page (Max 100).
 */
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
     * Compact constructor for sanitization and default values.
     */
    public ListHashesQuery {
        tenantId = tenantId != null ? tenantId.trim() : null;
        page = (page == null) ? 0 : page;
        size = (size == null) ? 20 : size;
    }
}