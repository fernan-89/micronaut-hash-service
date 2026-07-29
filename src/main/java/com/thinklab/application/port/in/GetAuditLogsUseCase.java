package com.thinklab.application.port.in;

import com.thinklab.domain.model.HashAudit;
import reactor.core.publisher.Flux;

import java.util.UUID;

/**
 * Application Port: Input boundary for the retrieval of immutable forensic audit trails.
 *
 * <p><b>Architectural Role:</b>
 * This interface defines the primary input port (use case contract) for querying the historical state
 * and forensic event timeline of business entity aggregates. It serves as a secure, non-blocking gateway
 * for compliance reporting, forensic auditing, and historical state reconstruction without exposing
 * persistence implementation details to inbound adapters.
 *
 * <p><b>Contractual Obligations:</b>
 * <ul>
 * <li><b>Reactive Non-Blocking Stream:</b> Delivers audit histories asynchronously via Project Reactor
 *     {@link Flux}, allowing downstream consumers to handle backpressure and high-volume data sets safely.</li>
 * <li><b>Identity Sovereignty:</b> Enforces native {@link UUID} parameters to maintain strict
 *     BSON Binary (Subtype 4) performance optimization across all query boundaries.</li>
 * <li><b>Compliance & Chronology:</b> Guarantees structured retrieval of audit events to maintain
 *     absolute historical integrity and observability.</li>
 * <li><b>Framework Independence:</b> Completely decoupled from infrastructure frameworks, relying solely on
 *     native Java types and standard reactive primitives.</li>
 * </ul>
 *
 * @author ThinkLab
 * @version 1.0.0
 * @since 1.0
 */
public interface GetAuditLogsUseCase {

    /**
     * Retrieves the complete, chronological forensic audit trail for a specified entity UUID.
     *
     * @param entityId The universally unique identifier (UUID) of the target business entity (e.g., HashToken ID). Must not be null.
     * @return A {@link Flux} emitting the sequence of immutable {@link HashAudit} records ordered by creation timestamp.
     * @throws NullPointerException if the provided entity UUID is null.
     */
    Flux<HashAudit> execute(UUID entityId);
}