package com.thinklab.infrastructure.adapter.out.mongo.entity;

import com.thinklab.domain.model.HashToken;
import com.thinklab.domain.valueobject.HashAlgorithm;
import com.thinklab.domain.valueobject.HashStatus;
import io.micronaut.core.annotation.Introspected;
import io.micronaut.data.annotation.*;
import io.micronaut.serde.annotation.Serdeable;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.time.Instant;
import java.util.UUID;

/**
 * Infrastructure Entity: Persistence model for the HashToken aggregate.
 * <p>Maps directly to the MongoDB "hash_token" collection. This entity serves as the
 * persistent representation of the domain aggregate, optimized for high-throughput
 * access and consistency verification.</p>
 *
 * <p><b>Architectural Principles (Mission-Critical Pattern):</b></p>
 * <ul>
 * <li><b>Anti-Corruption Layer (ACL):</b> Encapsulates persistence schema, decoupling the Domain from database specifics.</li>
 * <li><b>Data Integrity:</b> Utilizes optimistic locking via versioning to prevent data collisions.</li>
 * <li><b>Performance-Oriented:</b> Multi-dimensional indexing strategy for efficient lookup and state-based filtering.</li>
 * </ul>
 */
@Serdeable
@Introspected
@MappedEntity("hash_token")
@Indexes({
        @Index(columns = {"tenantId", "status"}),
        @Index(columns = {"tenantId", "payload"}),
        @Index(columns = {"tenantId", "generatedHash"})
})
public record HashTokenEntity(
        @Id
        UUID id,

        @Nonnull
        String tenantId,

        @Nonnull
        String sourceService,

        @Nonnull
        String payload,

        @Nonnull
        String generatedHash,

        @Nonnull
        HashAlgorithm algorithm,

        @Nonnull
        HashStatus status,

        @Nonnull
        String createdBy,

        @Nonnull
        Instant createdAt,

        @Nullable
        String updatedBy,

        @Nullable
        Instant updatedAt,

        @Version
        Long version
) {

    /**
     * Factory method to map a pure Domain Aggregate to this Persistence Entity.
     *
     * @param domain The domain aggregate instance.
     * @return A mapped infrastructure entity.
     */
    public static HashTokenEntity fromDomain(@Nonnull HashToken domain) {
        return new HashTokenEntity(
                UUID.fromString(domain.id()),
                domain.tenantId(),
                domain.sourceService(),
                domain.payload(),
                domain.generatedHash(),
                domain.algorithm(),
                domain.status(),
                domain.createdBy(),
                domain.createdAt(),
                domain.updatedBy(),
                domain.updatedAt(),
                domain.version()
        );
    }

    /**
     * Maps the persistence entity back to the pure Domain Aggregate.
     *
     * @return A domain aggregate instance.
     */
    public HashToken toDomain() {
        return new HashToken(
                id.toString(),
                tenantId,
                sourceService,
                payload,
                generatedHash,
                algorithm,
                status,
                createdBy,
                createdAt,
                updatedBy,
                updatedAt,
                version != null ? version : 0L
        );
    }
}