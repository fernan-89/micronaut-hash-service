package com.thinklab.infrastructure.adapter.out.mongo.repository;

import com.thinklab.infrastructure.adapter.out.mongo.entity.HashAuditEntity;
import io.micronaut.data.mongodb.annotation.MongoRepository;
import io.micronaut.data.repository.reactive.ReactorCrudRepository;
import reactor.core.publisher.Flux;

import java.util.UUID;

/**
 * Infrastructure Adapter: Reactive repository for {@link HashAuditEntity} persistence.
 *
 * <p><b>Architectural Role:</b>
 * This interface defines the low-level infrastructure contract for storing immutable forensic audit
 * records within MongoDB. It implements the persistence adapter pattern, leveraging Micronaut Data's
 * Ahead-of-Time (AOT) compilation to generate reflection-free, highly optimized non-blocking query routines.
 *
 * <p><b>Contractual Obligations:</b>
 * <ul>
 * <li><b>Non-Blocking I/O:</b> Extends {@link ReactorCrudRepository} to natively support Project Reactor
 *     streams ({@link Flux}), guaranteeing that Netty EventLoops are never stalled by disk I/O.</li>
 * <li><b>Identity Sovereignty:</b> Enforces {@link UUID} across primary keys, entity references, and
 *     transaction correlation parameters for seamless BSON Binary (Subtype 4) indexing and matching.</li>
 * <li><b>Append-Only Forensic Integrity:</b> Designed strictly for write and read operations. The domain
 *     layer explicitly prohibits updates or deletions of forensic audit histories.</li>
 * </ul>
 *
 * @author ThinkLab
 * @version 1.0.0
 * @since 1.0
 */
@MongoRepository
public interface HashAuditMongoRepository extends ReactorCrudRepository<HashAuditEntity, UUID> {

    /**
     * Retrieves a reactive stream of audit logs correlated to a specific transaction UUID.
     *
     * <p>Essential for reconstructing execution flows and tracing cascading operations across
     * distributed reactive services.
     *
     * @param txId The unique transaction correlation UUID. Must not be null.
     * @return A {@link Flux} emitting audit entities matching the transaction context.
     */
    Flux<HashAuditEntity> findByTxId(UUID txId);

    /**
     * Retrieves a reactive stream of audit logs scoped strictly to an isolated tenant,
     * ordered chronologically by creation timestamp in descending order (newest first).
     *
     * @param tenantId The isolated tenant boundary identifier. Must not be null or blank.
     * @return A {@link Flux} emitting the chronological forensic trail for the specified tenant.
     */
    Flux<HashAuditEntity> findByTenantIdOrderByTimestampDesc(String tenantId);

    /**
     * Retrieves the complete forensic audit trail mapped to a specific business entity aggregate.
     *
     * <p>Leverages native BSON Binary UUID indexing to achieve sub-millisecond retrieval speeds
     * for historical lifecycle reconstruction.
     *
     * @param entityId The universal unique identifier (UUID) of the target {@link com.thinklab.domain.model.HashToken}.
     * @return A {@link Flux} emitting all historical audit entities linked to the entity.
     */
    Flux<HashAuditEntity> findByEntityId(UUID entityId);
}