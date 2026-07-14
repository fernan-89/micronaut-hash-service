package com.thinklab.domain.model;

import com.thinklab.domain.valueobject.HashAlgorithm;
import com.thinklab.domain.valueobject.HashStatus;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.time.Instant;
import java.util.Objects;

/**
 * Domain Model: Aggregate Root representing a cryptographic hash or serial key.
 * <p>This record serves as the immutable definition of a HashToken. State transitions
 * are strictly governed by the underlying {@link HashStatus} state machine, ensuring
 * that the aggregate remains in a valid, consistent state throughout its lifecycle.</p>
 *
 * <p><b>Architectural Principles (Mission-Critical Pattern):</b></p>
 * <ul>
 * <li><b>Domain Purity:</b> Zero framework dependencies; infrastructure mapping is strictly excluded.</li>
 * <li><b>Functional Mutation:</b> State transitions return new instances, preserving immutability.</li>
 * <li><b>Invariant Enforcement:</b> Constructor-level validation prevents illegal domain states.</li>
 * </ul>
 */
public record HashToken(
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
     * Validates that the aggregate never enters an inconsistent state.
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
     * Factory method for creating a new HashToken instance.
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
     * @param executor Identification of the agent performing the action.
     * @return A new instance with updated status and audit fields.
     */
    @Nonnull
    public HashToken deactivate(@Nonnull String executor) {
        this.status.validateTransitionTo(HashStatus.INACTIVE);
        return new HashToken(
                id, tenantId, sourceService, payload, generatedHash,
                algorithm, HashStatus.INACTIVE, createdBy, createdAt,
                executor, Instant.now(), version
        );
    }

    /**
     * Transitions the token to ACTIVE state.
     *
     * @param executor Identification of the agent performing the action.
     * @return A new instance with updated status and audit fields.
     */
    @Nonnull
    public HashToken reactivate(@Nonnull String executor) {
        this.status.validateTransitionTo(HashStatus.ACTIVE);
        return new HashToken(
                id, tenantId, sourceService, payload, generatedHash,
                algorithm, HashStatus.ACTIVE, createdBy, createdAt,
                executor, Instant.now(), version
        );
    }

    /**
     * Transitions the token to REVOKED state (Terminal).
     *
     * @param executor Identification of the agent performing the action.
     * @return A new instance with updated status and audit fields.
     */
    @Nonnull
    public HashToken revoke(@Nonnull String executor) {
        this.status.validateTransitionTo(HashStatus.REVOKED);
        return new HashToken(
                id, tenantId, sourceService, payload, generatedHash,
                algorithm, HashStatus.REVOKED, createdBy, createdAt,
                executor, Instant.now(), version
        );
    }
}