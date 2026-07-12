package com.thinklab.domain.model;

import com.thinklab.domain.valueobject.HashAlgorithm;
import com.thinklab.domain.valueobject.HashStatus;
import io.micronaut.data.annotation.Id;
import io.micronaut.data.annotation.TypeDef;
import io.micronaut.data.model.DataType;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.time.Instant;
import java.util.Objects;

/**
 * Aggregate Root: Core domain model representing a cryptographic hash or serial key.
 * Modeled as an immutable Java Record, ensuring thread-safety and side-effect-free
 * domain operations. State transitions are strictly governed by the underlying
 * {@link HashStatus} state machine.
 *
 * <p><b>Architectural Rules:</b></p>
 * <ul>
 *     <li>Invariants are validated upon instantiation via the compact constructor.</li>
 *     <li>State mutations return a new instance (Functional Mutation).</li>
 *     <li>Concurrency is managed via versioning (Optimistic Locking).</li>
 * </ul>
 */
public record HashToken(
        @Id
        @TypeDef(type = DataType.UUID) // Força o Micronaut/Mongo a codificar a String como BSON Binary UUID
        @Nonnull String id,
        @Nonnull String tenantId,
        @Nonnull String sourceService,
        @Nonnull String payload,
        @Nonnull String generatedHash,
        @Nonnull HashAlgorithm algorithm,
        @Nonnull HashStatus status,
        @Nonnull String createdBy,
        @Nonnull Instant createdAt,
        @Nullable String updatedBy,
        @Nullable Instant updatedAt,
        @Nonnull Long version
) {

    /**
     * Compact constructor to enforce domain invariants.
     * Prevents the creation of a HashToken in an invalid state.
     */
    public HashToken {
        Objects.requireNonNull(id, "ID cannot be null.");
        Objects.requireNonNull(tenantId, "Tenant ID cannot be null.");
        Objects.requireNonNull(sourceService, "Source service cannot be null.");
        Objects.requireNonNull(payload, "Payload cannot be null.");
        Objects.requireNonNull(generatedHash, "Generated hash cannot be null.");
        Objects.requireNonNull(algorithm, "Algorithm cannot be null.");
        Objects.requireNonNull(status, "Status cannot be null.");
        Objects.requireNonNull(createdBy, "Creator identification is mandatory.");
        Objects.requireNonNull(createdAt, "Creation timestamp is mandatory.");
        Objects.requireNonNull(version, "Version field is required for concurrency control.");

        if (tenantId.isBlank()) throw new IllegalArgumentException("Tenant ID cannot be blank.");
        if (sourceService.isBlank()) throw new IllegalArgumentException("Source service cannot be blank.");
        if (generatedHash.isBlank()) throw new IllegalArgumentException("Generated hash cannot be blank.");
    }

    /**
     * Semantic static factory for creating a new HashToken instance.
     *
     * @param id            Unique identifier.
     * @param tenantId      Identifier for multi-tenant isolation.
     * @param sourceService Originating system name.
     * @param payload       Original content before hashing.
     * @param generatedHash Resulting cryptographic string.
     * @param algorithm     Strategy used for hashing.
     * @param creator       Identification of the executor.
     * @return An ACTIVE HashToken instance.
     */
    @Nonnull
    public static HashToken create(
            @Nonnull String id,
            @Nonnull String tenantId,
            @Nonnull String sourceService,
            @Nonnull String payload,
            @Nonnull String generatedHash,
            @Nonnull HashAlgorithm algorithm,
            @Nonnull String creator
    ) {
        return new HashToken(
                id,
                tenantId,
                sourceService,
                payload,
                generatedHash,
                algorithm,
                HashStatus.ACTIVE,
                creator,
                Instant.now(),
                null,
                null,
                0L
        );
    }

    /**
     * Transitions the token to INACTIVE state.
     *
     * @param executor Identification of the system or user performing the action.
     * @return A new instance of HashToken with updated status and audit fields.
     */
    @Nonnull
    public HashToken deactivate(@Nonnull String executor) {
        this.status.validateTransitionTo(HashStatus.INACTIVE);
        return new HashToken(
                id(), tenantId(), sourceService(), payload(), generatedHash(),
                algorithm(), HashStatus.INACTIVE, createdBy(), createdAt(),
                executor, Instant.now(), version()
        );
    }

    /**
     * Transitions the token back to ACTIVE state.
     *
     * @param executor Identification of the system or user performing the action.
     * @return A new instance of HashToken with updated status and audit fields.
     */
    @Nonnull
    public HashToken reactivate(@Nonnull String executor) {
        this.status.validateTransitionTo(HashStatus.ACTIVE);
        return new HashToken(
                id(), tenantId(), sourceService(), payload(), generatedHash(),
                algorithm(), HashStatus.ACTIVE, createdBy(), createdAt(),
                executor, Instant.now(), version()
        );
    }

    /**
     * Irreversibly revokes the token (Terminal state).
     *
     * @param executor Identification of the system or user performing the action.
     * @return A new instance of HashToken in REVOKED state.
     */
    @Nonnull
    public HashToken revoke(@Nonnull String executor) {
        this.status.validateTransitionTo(HashStatus.REVOKED);
        return new HashToken(
                id(), tenantId(), sourceService(), payload(), generatedHash(),
                algorithm(), HashStatus.REVOKED, createdBy(), createdAt(),
                executor, Instant.now(), version()
        );
    }
}