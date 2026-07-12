package com.thinklab.domain.model;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Domain Model: Represents an immutable audit entry for cryptographic operations.
 * This record ensures "Append-Only" integrity, providing a high-fidelity audit trail
 * for security forensics and business compliance.
 *
 * <p><b>Architectural Rules:</b></p>
 * <ul>
 *     <li>Strictly immutable: Audit records cannot be modified once created.</li>
 *     <li>Pure Java: Zero framework dependencies to ensure domain portability.</li>
 *     <li>Standardized: Uses a consistent metadata map for flexible context capture.</li>
 * </ul>
 */
public record HashAudit(
        @Nonnull String id,
        @Nonnull String txId,
        @Nonnull String tenantId,
        @Nonnull String operation,
        @Nonnull String status,
        @Nonnull String executorId,
        @Nonnull Instant timestamp,
        @Nonnull Map<String, Object> metadata
) {

    /**
     * Compact constructor for invariant validation.
     * Guarantees that every audit log has the minimum required context.
     */
    public HashAudit {
        Objects.requireNonNull(id, "Audit ID cannot be null.");
        Objects.requireNonNull(txId, "Transaction ID cannot be null.");
        Objects.requireNonNull(tenantId, "Tenant ID cannot be null.");
        Objects.requireNonNull(operation, "Operation type is mandatory.");
        Objects.requireNonNull(status, "Operation status is mandatory.");
        Objects.requireNonNull(executorId, "Executor identification is required.");
        Objects.requireNonNull(timestamp, "Audit timestamp is mandatory.");

        // Metadata is initialized as empty if null, ensuring no NullPointerExceptions in downstream logic.
        metadata = metadata != null ? Map.copyOf(metadata) : Collections.emptyMap();
    }

    /**
     * Static factory method to create a new audit record with default initial values.
     *
     * @param tenantId   The owner of the audited resource.
     * @param operation  The business action performed (e.g., CREATION, DEACTIVATION).
     * @param status     The outcome of the operation (SUCCESS, FAILURE).
     * @param executorId The system or user ID that authorized the action.
     * @param metadata   Additional key-value pairs for context (e.g., reason, IP address).
     * @return A fully initialized HashAudit instance.
     */
    @Nonnull
    public static HashAudit create(
            @Nonnull String tenantId,
            @Nonnull String operation,
            @Nonnull String status,
            @Nonnull String executorId,
            @Nullable Map<String, Object> metadata
    ) {
        return new HashAudit(
                UUID.randomUUID().toString(),
                UUID.randomUUID().toString(),
                tenantId,
                operation,
                status,
                executorId,
                Instant.now(),
                metadata
        );
    }
}