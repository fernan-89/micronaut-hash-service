package com.thinklab.infrastructure.adapter.out.mongo.entity;

import com.thinklab.domain.model.HashAudit;
import io.micronaut.core.annotation.Introspected;
import io.micronaut.data.annotation.GeneratedValue;
import io.micronaut.data.annotation.Id;
import io.micronaut.data.annotation.Index;
import io.micronaut.data.annotation.Indexes;
import io.micronaut.data.annotation.MappedEntity;
import io.micronaut.serde.annotation.Serdeable;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Infrastructure Entity: Persistence model for the forensic audit trail.
 * <p>Maps directly to the MongoDB "hash_audit" collection. This entity implements the
 * Append-Only principle, serving as a high-integrity container for immutable forensic event history.</p>
 *
 * <p><b>Architectural Principles (Mission-Critical Pattern):</b></p>
 * <ul>
 * <li><b>Anti-Corruption Layer (ACL):</b> Encapsulates persistence schema, decoupling the Domain from database specifics.</li>
 * <li><b>Forensic Integrity:</b> Enforces append-only semantics to maintain the immutable audit trail.</li>
 * <li><b>Performance-Oriented:</b> Uses BSON Binary Subtype 4 for efficient entity correlation.</li>
 * <li><b>Observability:</b> Provides multi-dimensional indexing for real-time audit retrieval.</li>
 * </ul>
 */
@Serdeable
@Introspected
@MappedEntity("hash_audit")
@Indexes({
        @Index(columns = {"txId"}),
        @Index(columns = {"tenantId"}),
        @Index(columns = {"entityId"}),
        @Index(columns = {"executorId"}),
        @Index(columns = {"timestamp"})
})
public record HashAuditEntity(
        @Id
        @GeneratedValue
        String id,

        @Nonnull
        String txId,

        @Nonnull
        String tenantId,

        @Nonnull
        UUID entityId,

        @Nonnull
        String operation,

        @Nonnull
        String status,

        @Nonnull
        String executorId,

        @Nonnull
        Instant timestamp,

        @Nullable
        Map<String, Object> metadata
) {

    /**
     * Factory method to map a pure Domain Audit model to this Persistence Entity.
     *
     * @param domain The domain aggregate record.
     * @return A mapped infrastructure entity instance.
     */
    public static HashAuditEntity fromDomain(@Nonnull HashAudit domain) {
        return new HashAuditEntity(
                domain.id(),
                domain.txId(),
                domain.tenantId(),
                UUID.fromString(domain.entityId()),
                domain.operation(),
                domain.status(),
                domain.executorId(),
                domain.timestamp(),
                domain.metadata()
        );
    }

    /**
     * Maps the persistence entity back to the pure Domain Audit model.
     *
     * @return A domain record instance.
     */
    public HashAudit toDomain() {
        return new HashAudit(
                id,
                txId,
                tenantId,
                entityId.toString(),
                operation,
                status,
                executorId,
                timestamp,
                metadata != null ? Map.copyOf(metadata) : Map.of()
        );
    }
}