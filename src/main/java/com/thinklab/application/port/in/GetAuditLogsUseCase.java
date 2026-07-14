package com.thinklab.application.port.in;

import com.thinklab.domain.model.HashAudit;
import io.micronaut.core.annotation.NonNull;
import reactor.core.publisher.Flux;

/**
 * Application Port: Input boundary for the retrieval of forensic audit trails.
 * <p>This interface defines the use case contract for querying the historical state
 * and event timeline of business entities. It serves as a secure, non-blocking gateway
 * for compliance reporting, forensic analysis, and historical state reconstruction.</p>
 *
 * <p><b>Architectural Principles (Mission-Critical Pattern):</b></p>
 * <ul>
 * <li><b>Non-blocking:</b> Fully integrated into the Project Reactor pipeline, supporting high-throughput stream processing of large datasets.</li>
 * <li><b>Compliance-Ready:</b> Ensures chronological data access to maintain the integrity of the forensic audit trail.</li>
 * <li><b>Isolation:</b> Decouples the consumer (e.g., API Controllers) from the underlying persistence strategy of audit records.</li>
 * <li><b>Stream-Oriented:</b> Efficiently handles potentially extensive audit logs via reactive backpressure.</li>
 * </ul>
 *
 * @version 1.0.0
 */
public interface GetAuditLogsUseCase {

    /**
     * Retrieves the complete, chronological forensic audit trail for a specified entity.
     *
     * @param entityId The unique identifier of the target business entity (e.g., HashToken ID).
     * @return A {@link Flux} emitting the sequence of {@link HashAudit} records ordered by creation timestamp.
     * @throws NullPointerException if the provided entityId is null, preserving pipeline integrity (Fail-Fast).
     */
    @NonNull
    Flux<HashAudit> execute(@NonNull String entityId);
}