package com.thinklab.application.port.in;

import com.thinklab.domain.model.HashAudit;
import io.micronaut.core.annotation.NonNull;
import reactor.core.publisher.Flux;

/**
 * Input Port: GetAuditLogsUseCase.
 * <p>Defines the contract for retrieving the forensic audit trail associated
 * with a specific business entity (e.g., HashToken).</p>
 *
 * <p><b>Architectural Roles:</b></p>
 * <ul>
 * <li><b>Separation of Concerns:</b> The Domain Layer uses this to request
 * historical data without being coupled to the underlying persistence implementation.</li>
 * <li><b>Reactive Contract:</b> Returns a {@link Flux} of {@link HashAudit} items,
 * supporting non-blocking stream processing of potentially large audit logs.</li>
 * </ul>
 */
public interface GetAuditLogsUseCase {

    /**
     * Retrieves the chronological audit trail for a given entity identifier.
     *
     * @param entityId The unique identifier of the entity whose audit trail is requested.
     * @return A {@link Flux} of {@link HashAudit} records, ordered chronologically.
     * @throws IllegalArgumentException if the provided entityId is blank or malformed.
     */
    @NonNull
    Flux<HashAudit> execute(@NonNull String entityId);
}