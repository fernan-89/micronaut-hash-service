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
 * <p><b>Architectural Principles (Mission-Critical Pattern):</b></p>
 * <ul>
 * <li><b>Reactive Orchestration:</b> Implements non-blocking retrieval of audit streams.</li>
 * <li><b>Invariant Protection:</b> Validates input parameters (Fail-Fast) before triggering repository operations.</li>
 * <li><b>Telemetry:</b> Structured logging tags track the lifecycle of the forensic retrieval pipeline.</li>
 * </ul>
 *
 * @version 1.0.0
 */
@Slf4j
@Singleton
@RequiredArgsConstructor
public class GetAuditLogsInteractor implements GetAuditLogsUseCase {

    private final HashAuditRepositoryPort auditRepository;

    /**
     * Executes the retrieval of the immutable audit trail for a specified entity.
     *
     * @param entityId The unique system identifier for which the audit trail is requested.
     * @return A {@link Flux} of {@link HashAudit} records.
     * @throws IllegalArgumentException if the provided entityId is null or blank (Fail-Fast).
     */
    @Override
    @NonNull
    public Flux<HashAudit> execute(@NonNull String entityId) {
        Objects.requireNonNull(entityId, "Entity ID cannot be null.");

        return Flux.defer(() -> {
            if (entityId.isBlank()) {
                log.warn("[ACTION: GET_AUDIT] - Orchestration halted: Entity ID is blank.");
                return Flux.error(new IllegalArgumentException("Entity ID cannot be blank."));
            }

            return auditRepository.findByEntityId(entityId)
                    .doOnSubscribe(s -> log.info("[ACTION: GET_AUDIT] [ID: {}] - Initiating forensic trail retrieval.", entityId))
                    .doOnComplete(() -> log.info("[ACTION: GET_AUDIT] [ID: {}] - Forensic trail retrieval completed successfully.", entityId))
                    .doOnError(error -> log.error("[ACTION: GET_AUDIT] [ID: {}] - CRITICAL: Forensic trail retrieval failed: {}", entityId, error.getMessage()));
        });
    }
}