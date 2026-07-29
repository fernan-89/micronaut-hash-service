package com.thinklab.application.interactor;

import com.thinklab.application.port.in.GetAuditLogsUseCase;
import com.thinklab.application.port.out.HashAuditRepositoryPort;
import com.thinklab.domain.model.HashAudit;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;

import java.util.Objects;
import java.util.UUID;

/**
 * Application Interactor: Implementation of the {@link GetAuditLogsUseCase} input port.
 *
 * <p><b>Architectural Role:</b>
 * This interactor orchestrates the business requirement for retrieving an immutable forensic audit trail.
 * It strictly adheres to Hexagonal Architecture by acting as a decoupled application service,
 * ensuring data access details remain entirely encapsulated behind the {@link HashAuditRepositoryPort}.
 *
 * <p><b>Contractual Obligations:</b>
 * <ul>
 * <li><b>Constructor Injection (ADR-001):</b> Utilizes an explicit {@link Inject} constructor to guarantee
 *     deterministic bean wiring and AOP proxy reliability.</li>
 * <li><b>Identity Sovereignty:</b> Enforces native {@link UUID} parameters to maintain strict
 *     BSON Binary (Subtype 4) performance optimization across database boundaries.</li>
 * <li><b>Reactive Non-Blocking Stream:</b> Delivers audit histories asynchronously via Project Reactor
 *     {@link Flux}, safeguarding Netty EventLoops.</li>
 * <li><b>Fail-Fast Invariant Protection:</b> Defensively rejects null identifiers before executing repository queries.</li>
 * </ul>
 *
 * @author ThinkLab
 * @version 1.0.0
 * @since 1.0
 */
@Slf4j
@Singleton
public class GetAuditLogsInteractor implements GetAuditLogsUseCase {

    private final HashAuditRepositoryPort auditRepository;

    /**
     * Explicit constructor for strict dependency injection (ADR-001).
     *
     * @param auditRepository The outbound port for audit persistence and retrieval. Must not be null.
     * @throws NullPointerException if the audit repository port is null.
     */
    @Inject
    public GetAuditLogsInteractor(HashAuditRepositoryPort auditRepository) {
        this.auditRepository = Objects.requireNonNull(auditRepository, "Application constraint violated: HashAuditRepositoryPort cannot be null.");
    }

    /**
     * Executes the retrieval of the immutable forensic audit trail for a specified entity UUID.
     *
     * @param entityId The universally unique identifier (UUID) of the target domain entity. Must not be null.
     * @return A {@link Flux} emitting immutable {@link HashAudit} records in chronological order.
     * @throws NullPointerException if the provided entity UUID is null.
     */
    @Override
    public Flux<HashAudit> execute(UUID entityId) {
        Objects.requireNonNull(entityId, "Application constraint violated: Entity UUID cannot be null for forensic retrieval.");

        return Flux.defer(() -> auditRepository.findByEntityId(entityId)
                .doOnSubscribe(s -> log.info("[ACTION: GET_AUDIT] [ID: {}] - Initiating forensic trail retrieval.", entityId))
                .doOnComplete(() -> log.info("[ACTION: GET_AUDIT] [ID: {}] - Forensic trail retrieval completed successfully.", entityId))
                .doOnError(error -> log.error("[ACTION: GET_AUDIT] [ID: {}] - CRITICAL: Forensic trail retrieval failed: {}", entityId, error.getMessage(), error))
        );
    }
}