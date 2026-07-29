package com.thinklab.application.port.out;

import com.thinklab.domain.model.HashAudit;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Application Port: Output boundary for the Audit persistence layer.
 *
 * <p><b>Architectural Role:</b>
 * This interface defines the outbound persistence contract (driven port) for storing and retrieving
 * immutable forensic audit records. Designed for high-assurance compliance environments, this port
 * ensures that security-critical events are persisted effectively without coupling core domain logic
 * to underlying database engines or storage drivers.
 *
 * <p><b>Contractual Obligations:</b>
 * <ul>
 * <li><b>Reactive Non-Blocking Stream:</b> Operates strictly within the Project Reactor pipeline using
 *     {@link Mono} and {@link Flux}, safeguarding Netty EventLoops from disk I/O blocking.</li>
 * <li><b>Identity Sovereignty:</b> Enforces native {@link UUID} parameters across transaction correlations
 *     and entity references to maintain strict BSON Binary (Subtype 4) index compatibility.</li>
 * <li><b>Append-Only Forensic Immutability:</b> Assumes a strict write-once compliance storage model
 *     prohibiting updates or deletions of historical audit trails.</li>
 * <li><b>Framework Independence:</b> Completely decoupled from infrastructure frameworks, relying solely on
 *     native Java types and standard reactive primitives.</li>
 * </ul>
 *
 * @author ThinkLab
 * @version 1.0.0
 * @since 1.0
 */
public interface HashAuditRepositoryPort {

    /**
     * Persists an immutable forensic audit record to the underlying storage engine in a non-blocking manner.
     *
     * @param audit The immutable {@link HashAudit} domain instance containing event details. Must not be null.
     * @return A {@link Mono} emitting the successfully persisted domain audit record.
     * @throws NullPointerException if the provided audit object is null.
     */
    Mono<HashAudit> save(HashAudit audit);

    /**
     * Retrieves audit records associated with a specific transaction correlation UUID.
     *
     * @param txId The unique transaction correlation UUID. Must not be null.
     * @return A {@link Flux} emitting audit records matching the transaction context.
     * @throws NullPointerException if the provided transaction UUID is null.
     */
    Flux<HashAudit> findByTxId(UUID txId);

    /**
     * Retrieves all audit records scoped strictly to an isolated tenant boundary, facilitating
     * multi-tenant compliance reporting and forensic auditing.
     *
     * @param tenantId The isolated tenant boundary identifier. Must not be null or blank.
     * @return A {@link Flux} emitting all chronological audit records associated with the tenant.
     * @throws NullPointerException if the tenant ID is null.
     */
    Flux<HashAudit> findByTenantId(String tenantId);

    /**
     * Retrieves the history of forensic audit events for a specific domain entity UUID
     * (e.g., a specific HashToken aggregate instance).
     *
     * @param entityId The universally unique identifier (UUID) of the target domain entity. Must not be null.
     * @return A {@link Flux} emitting the forensic audit trail for the specified entity.
     * @throws NullPointerException if the provided entity UUID is null.
     */
    Flux<HashAudit> findByEntityId(UUID entityId);
}