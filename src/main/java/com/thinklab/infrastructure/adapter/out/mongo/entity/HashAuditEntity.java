package com.thinklab.infrastructure.adapter.out.mongo.entity;

import com.thinklab.domain.model.HashAudit;
import io.micronaut.core.annotation.Introspected;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.data.annotation.GeneratedValue;
import io.micronaut.data.annotation.Id;
import io.micronaut.data.annotation.Index;
import io.micronaut.data.annotation.Indexes;
import io.micronaut.data.annotation.MappedEntity;
import io.micronaut.serde.annotation.Serdeable;

import java.time.Instant;
import java.util.Map;

/**
 * Infrastructure Entity: Persistence model for the forensic audit trail.
 * This record maps directly to the MongoDB "hash_audit" collection. It is designed
 * following the Append-Only principle to ensure the integrity of the audit history.
 *
 * <p><b>Persistence Strategy:</b></p>
 * <ul>
 *     <li><b>Immutable:</b> Implemented as a Java Record for thread-safety.</li>
 *     <li><b>BSON Decoupling:</b> Uses String identifiers to shield higher layers from driver types.</li>
 *     <li><b>Forensic Optimized:</b> Indexed for rapid retrieval by tenant, transaction, or executor.</li>
 * </ul>
 */
@Serdeable
@Introspected
@MappedEntity("hash_audit")
@Indexes({
        @Index(columns = {"txId"}),
        @Index(columns = {"tenantId"}),
        @Index(columns = {"executorId"}),
        @Index(columns = {"timestamp"})
})
public record HashAuditEntity(
        @Id
        @GeneratedValue
        String id,

        @NonNull
        String txId,

        @NonNull
        String tenantId,

        @NonNull
        String operation,

        @NonNull
        String status,

        @NonNull
        String executorId,

        @NonNull
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
    public static HashAuditEntity fromDomain(@NonNull HashAudit domain) {
        return new HashAuditEntity(
                domain.id(),
                domain.txId(),
                domain.tenantId(),
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
                operation,
                status,
                executorId,
                timestamp,
                metadata != null ? Map.copyOf(metadata) : Map.of()
        );
    }
}