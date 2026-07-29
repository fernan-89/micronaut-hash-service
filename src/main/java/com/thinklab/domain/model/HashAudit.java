package com.thinklab.domain.model;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Domain Model: Immutable aggregate representing a forensic cryptographic audit event.
 *
 * <p><b>Architectural Role:</b>
 * This record serves as the absolute, immutable source of truth for all security-critical
 * lifecycle operations performed on a {@link HashToken}. It constitutes the "Audit" half of
 * the CQRS "360° Full View" projection pattern.
 *
 * <p><b>Contractual Obligations:</b>
 * <ul>
 *   <li><b>Forensic Immutability:</b> Asserts that an audit history cannot be mutated post-instantiation.
 *       Embedded metadata collections are securely locked via defensive copying.</li>
 *   <li><b>Identity Sovereignty:</b> Ensures all structural identifiers (Audit ID, Transaction ID,
 *       Entity ID) strictly employ Universal Unique Identifiers (UUIDs) for BSON Binary compliance.</li>
 *   <li><b>Domain Purity:</b> Contains zero infrastructure dependencies, preserving strict isolation
 *       from the application and persistence tiers.</li>
 * </ul>
 *
 * <p><b>Concurrency & Thread-Safety:</b>
 * As a natively immutable Java Record containing only immutable references, this object is strictly
 * thread-safe. It is fully optimized for cross-thread publishing within Netty EventLoops and parallel
 * aggregations utilizing Project Reactor's {@code Mono.zip()} operator.
 *
 * @param id         The globally unique identifier for this specific audit entry.
 * @param txId       The reactive correlation identifier binding this audit to the encompassing transaction context.
 * @param tenantId   The isolated multi-tenant boundary owning the audited resource.
 * @param entityId   The deterministic UUID of the {@link HashToken} being audited.
 * @param operation  The distinct business operation executed (e.g., "CREATION", "REVOCATION").
 * @param status     The definitive outcome of the operation (e.g., "SUCCESS", "COMPLIANCE_FAILURE").
 * @param executorId The verified identity (system or human) authorizing the cryptographic action.
 * @param timestamp  The exact UTC instant the state transition materialized.
 * @param metadata   Rich contextual payload for SIEM (Security Information and Event Management) analytics.
 *
 * @author ThinkLab
 * @since 1.0
 */
public record HashAudit(
        UUID id,
        UUID txId,
        String tenantId,
        UUID entityId,
        String operation,
        String status,
        String executorId,
        Instant timestamp,
        Map<String, Object> metadata
) {

    /**
     * Compact constructor enforcing absolute domain invariants and forensic integrity.
     *
     * <p><b>Contract:</b> Defensively guarantees that no invalid, null, or logically empty state
     * can poison the audit trail.
     *
     * @throws NullPointerException if any mandatory parameter is null, or if the metadata map contains null keys/values.
     * @throws IllegalArgumentException if any mandatory string parameter is blank.
     */
    public HashAudit {
        Objects.requireNonNull(id, "Domain Invariant Violation: Audit ID cannot be null.");
        Objects.requireNonNull(txId, "Domain Invariant Violation: Transaction ID cannot be null.");
        Objects.requireNonNull(tenantId, "Domain Invariant Violation: Tenant ID cannot be null.");
        Objects.requireNonNull(entityId, "Domain Invariant Violation: Entity ID cannot be null.");
        Objects.requireNonNull(operation, "Domain Invariant Violation: Operation type cannot be null.");
        Objects.requireNonNull(status, "Domain Invariant Violation: Operation status cannot be null.");
        Objects.requireNonNull(executorId, "Domain Invariant Violation: Executor ID cannot be null.");
        Objects.requireNonNull(timestamp, "Domain Invariant Violation: Audit timestamp cannot be null.");

        if (tenantId.isBlank()) {
            throw new IllegalArgumentException("Domain Invariant Violation: Tenant ID cannot be blank.");
        }
        if (operation.isBlank()) {
            throw new IllegalArgumentException("Domain Invariant Violation: Operation cannot be blank.");
        }
        if (status.isBlank()) {
            throw new IllegalArgumentException("Domain Invariant Violation: Status cannot be blank.");
        }
        if (executorId.isBlank()) {
            throw new IllegalArgumentException("Domain Invariant Violation: Executor ID cannot be blank.");
        }

        // Defensively copy the map to ensure absolute immutability.
        // Map.copyOf will intrinsically reject null keys and null values, safeguarding the payload.
        metadata = (metadata == null || metadata.isEmpty())
                ? Collections.emptyMap()
                : Map.copyOf(metadata);
    }

    /**
     * Factory method for provisioning a newly materialized forensic audit record.
     *
     * @param txId       The transaction correlation identifier injected from the Reactive Context.
     * @param tenantId   The owner of the audited resource.
     * @param entityId   The targeted business entity identifier (HashToken UUID).
     * @param operation  The business action performed.
     * @param status     The outcome of the operation.
     * @param executorId The system or user ID that authorized the action.
     * @param metadata   Additional key-value pairs for context (can be null).
     * @return A fully initialized, thread-safe {@link HashAudit} instance.
     */
    public static HashAudit create(
            UUID txId,
            String tenantId,
            UUID entityId,
            String operation,
            String status,
            String executorId,
            Map<String, Object> metadata
    ) {
        return new HashAudit(
                UUID.randomUUID(),
                txId,
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