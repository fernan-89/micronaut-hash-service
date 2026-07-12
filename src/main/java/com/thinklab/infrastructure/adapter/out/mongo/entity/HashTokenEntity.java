package com.thinklab.infrastructure.adapter.out.mongo.entity;

import com.thinklab.domain.model.HashToken;
import com.thinklab.domain.valueobject.HashAlgorithm;
import com.thinklab.domain.valueobject.HashStatus;
import io.micronaut.core.annotation.Introspected;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.data.annotation.Id;
import io.micronaut.data.annotation.Version;
import io.micronaut.data.annotation.MappedEntity;
import io.micronaut.serde.annotation.Serdeable;

import java.time.Instant;

/**
 * Infrastructure Entity: Persistence model for the HashToken aggregate.
 * This record maps directly to the MongoDB "hash_token" collection.
 */
@Serdeable
@Introspected
@MappedEntity("hash_token")
public record HashTokenEntity(
        @Id
        String id,

        @NonNull
        String tenantId,

        @NonNull
        String sourceService,

        @NonNull
        String payload,

        @NonNull
        String generatedHash,

        @NonNull
        HashAlgorithm algorithm,

        @NonNull
        HashStatus status,

        @NonNull
        String createdBy,

        @NonNull
        Instant createdAt,

        @Nullable
        String updatedBy,

        @Nullable
        Instant updatedAt,

        @Version
        Long version
) {

    /**
     * Maps a domain model to a persistence entity.
     */
    public static HashTokenEntity fromDomain(@NonNull HashToken domain) {
        return new HashTokenEntity(
                domain.id(),
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
     * Maps a persistence entity back to the domain model.
     */
    public HashToken toDomain() {
        return new HashToken(
                id,
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
