package com.thinklab.domain.model;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Domain Model: Immutable aggregate representing a forensic audit event.
 * <p>This model serves as the source of truth for all security-critical operations,
 * guaranteeing the integrity of the audit trail through strictly immutable state.
 * It is designed to be framework-agnostic, ensuring portability across the
 * Application and Infrastructure layers.</p>
 *
 * <p><b>Architectural Principles (Mission-Critical Pattern):</b></p>
 * <ul>
 * <li><b>Immutability:</b> Ensures that audit history cannot be altered post-persistence, satisfying forensic requirements.</li>
 * <li><b>Domain Portability:</b> Zero framework dependencies, allowing the logic to be reused across different infrastructure contexts.</li>
 * <li><b>Defensive Integrity:</b> Validates all invariants at the point of instantiation to prevent corrupted domain states.</li>
 * </ul>
 *
 * @param id         The unique identifier for this specific audit entry.
 * @param txId       The correlation identifier for the encompassing transaction.
 * @param tenantId   The owner of the audited resource.
 * @param entityId   The identifier of the business entity being audited.
 * @param operation  The business operation type (e.g., CREATION, DEACTIVATION).
 * @param status     The outcome of the operation (e.g., SUCCESS, FAILURE).
 * @param executorId The identifier of the agent (user or system) authorizing the action.
 * @param timestamp  The UTC instant when the event occurred.
 * @param metadata   Contextual data providing additional details for forensic analysis.
 */
public record HashAudit(
        @Nonnull String id,
        @Nonnull String txId,
        @Nonnull String tenantId,
        @Nonnull String entityId,
        @Nonnull String operation,
        @Nonnull String status,
        @Nonnull String executorId,
        @Nonnull Instant timestamp,
        @Nonnull Map<String, Object> metadata
) {

    /**
     * Compact constructor enforcing domain invariants.
     * Ensures that all required fields are non-null and that metadata is
     * treated as an immutable map to prevent post-instantiation modification.
     */
    public HashAudit {
        Objects.requireNonNull(id, "Audit ID cannot be null.");
        Objects.requireNonNull(txId, "Transaction ID cannot be null.");
        Objects.requireNonNull(tenantId, "Tenant ID cannot be null.");
        Objects.requireNonNull(entityId, "Entity ID cannot be null.");
        Objects.requireNonNull(operation, "Operation type is mandatory.");
        Objects.requireNonNull(status, "Operation status is mandatory.");
        Objects.requireNonNull(executorId, "Executor identification is mandatory.");
        Objects.requireNonNull(timestamp, "Audit timestamp is mandatory.");

        metadata = metadata != null ? Map.copyOf(metadata) : Collections.emptyMap();
    }

    /**
     * Factory method for creating a new audit record.
     * Generates system identifiers and timestamps automatically.
     *
     * @param tenantId   The owner of the audited resource.
     * @param entityId   The targeted business entity identifier.
     * @param operation  The business action performed.
     * @param status     The outcome of the operation.
     * @param executorId The system or user ID that authorized the action.
     * @param metadata   Additional key-value pairs for context.
     * @return A fully initialized, immutable {@link HashAudit} instance.
     */
    @Nonnull
    public static HashAudit create(
            @Nonnull String tenantId,
            @Nonnull String entityId,
            @Nonnull String operation,
            @Nonnull String status,
            @Nonnull String executorId,
            @Nullable Map<String, Object> metadata
    ) {
        return new HashAudit(
                UUID.randomUUID().toString(),
                UUID.randomUUID().toString(),
                tenantId,
                entityId,
                operation,
                status,
                executorId,
                Instant.now(),
                metadata
        );
    }
}