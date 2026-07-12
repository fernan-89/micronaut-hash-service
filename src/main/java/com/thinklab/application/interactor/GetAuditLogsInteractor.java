package com.thinklab.application.interactor;

import com.thinklab.application.port.in.GetAuditLogsUseCase;
import com.thinklab.application.port.out.HashAuditRepositoryPort;
import com.thinklab.domain.model.HashAudit;
import io.micronaut.core.annotation.NonNull;
import jakarta.inject.Singleton;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import java.util.Objects;

/**
 * Application Interactor: Implementation of the {@link GetAuditLogsUseCase} input port.
 * <p>This service handles the business requirement for retrieving a forensic audit trail.
 * It strictly adheres to the Clean Architecture principles by acting as an orchestrator,
 * ensuring that data access details remain encapsulated behind the {@link HashAuditRepositoryPort}.</p>
 *
 * <p><b>Architectural Roles:</b></p>
 * <ul>
 * <li><b>Reactive Orquestration:</b> Implements non-blocking retrieval of audit streams.</li>
 * <li><b>Invariant Protection:</b> Validates input parameters (e.g., non-blank entity IDs)
 * before triggering repository operations.</li>
 * <li><b>Observability:</b> Logs forensic retrieval attempts for system traceability.</li>
 * </ul>
 */
@Slf4j
@Singleton
@RequiredArgsConstructor
public class GetAuditLogsInteractor implements GetAuditLogsUseCase {

    private final HashAuditRepositoryPort auditRepository;

    /**
     * Executes the retrieval of the audit trail for a specified entity.
     *
     * @param entityId The unique system identifier for which the audit trail is requested.
     * @return A {@link Flux} of {@link HashAudit} records.
     * @throws IllegalArgumentException if the provided entityId is null or blank.
     */
    @Override
    @NonNull
    public Flux<HashAudit> execute(@NonNull String entityId) {
        log.debug("Application Request: Retrieving audit logs for entity ID [{}]", entityId);

        if (Objects.requireNonNull(entityId, "Entity ID cannot be null").isBlank()) {
            log.error("Application Error: Attempted to retrieve audit logs with blank ID");
            return Flux.error(new IllegalArgumentException("Entity ID cannot be blank"));
        }

        return auditRepository.findByEntityId(entityId)
                .doOnComplete(() -> log.debug("Application Success: Audit trail retrieved for ID [{}]", entityId))
                .doOnError(e -> log.error("Application Error: Failed to retrieve audit trail for ID [{}]: {}", entityId, e.getMessage()));
    }
}