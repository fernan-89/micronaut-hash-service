package com.thinklab.application.interactor;

import com.thinklab.application.usecase.command.DeactivateHashCommand;
import com.thinklab.application.port.out.HashAuditRepositoryPort;
import com.thinklab.application.port.out.HashTokenRepositoryPort;
import com.thinklab.domain.exception.HashNotFoundException;
import com.thinklab.domain.model.HashAudit;
import com.thinklab.domain.model.HashToken;
import com.thinklab.application.port.in.DeactivateHashUseCase;
import jakarta.annotation.Nonnull;
import jakarta.inject.Singleton;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.Objects;

/**
 * Application Interactor: Implementation of the {@link DeactivateHashUseCase} input port.
 * <p>This service orchestrates the business process of deactivating a cryptographic hash registry.
 * It coordinates the interaction between the domain state machine, the persistence
 * layer, and the mandatory audit trail required for security compliance.</p>
 *
 * <p><b>Architectural Principles (Mission-Critical Pattern):</b></p>
 * <ul>
 * <li><b>Non-blocking:</b> Operates strictly within the Project Reactor pipeline.</li>
 * <li><b>Stateless:</b> Maintains no internal state, ensuring thread-safety across concurrent requests.</li>
 * <li><b>Audit-Mandatory:</b> Every successful deactivation transactionally triggers a forensic audit record.</li>
 * <li><b>Telemetry:</b> Structured logging tags track the lifecycle of the orchestration pipeline.</li>
 * </ul>
 *
 * @version 1.0.0
 */
@Slf4j
@Singleton
@RequiredArgsConstructor
public class DeactivateHashInteractor implements DeactivateHashUseCase {

    private final HashTokenRepositoryPort hashTokenRepository;
    private final HashAuditRepositoryPort hashAuditRepository;

    /**
     * Orchestrates the deactivation of a HashToken and ensures the generation of an immutable audit trail.
     *
     * @param command The {@link DeactivateHashCommand} encapsulating the target identifier, executor, and justification.
     * @return A {@link Mono} emitting the mutated {@link HashToken} in its new INACTIVE state.
     * @throws NullPointerException if the provided command is null, preserving pipeline integrity (Fail-Fast).
     * @apiNote Emits a {@link HashNotFoundException} signal through the reactive stream if the target entity does not exist.
     */
    @Override
    @Nonnull
    public Mono<HashToken> execute(@Nonnull DeactivateHashCommand command) {
        Objects.requireNonNull(command, "DeactivateHashCommand cannot be null.");

        return hashTokenRepository.findById(command.hashId())
                .switchIfEmpty(Mono.defer(() -> {
                    log.warn("[ACTION: DEACTIVATE_HASH] [ID: {}] - Orchestration halted: Entity not found.", command.hashId());
                    return Mono.error(new HashNotFoundException(command.hashId()));
                }))
                .map(existingToken -> existingToken.deactivate(command.executor()))
                .flatMap(hashTokenRepository::update)
                .flatMap(updatedToken -> createAuditLog(updatedToken, command.executor(), command.reason())
                        .thenReturn(updatedToken))
                .doOnSubscribe(s -> log.info("[ACTION: DEACTIVATE_HASH] [ID: {}] [EXECUTOR: {}] - Initiating orchestration pipeline for status suspension.", command.hashId(), command.executor()))
                .doOnSuccess(token -> log.info("[ACTION: DEACTIVATE_HASH] [ID: {}] - Orchestration completed. Entity deactivated and forensic audit successfully persisted.", token.id()))
                .doOnError(error -> {
                    if (!(error instanceof HashNotFoundException)) {
                        log.error("[ACTION: DEACTIVATE_HASH] [ID: {}] - CRITICAL: Pipeline orchestration failed due to system exception: {}", command.hashId(), error.getMessage());
                    }
                });
    }

    /**
     * Constructs and persists an immutable forensic audit record for the deactivation lifecycle event.
     *
     * @param token    The newly deactivated {@link HashToken} entity.
     * @param executor The principal identifier of the user or system executing the action.
     * @param reason   The business justification provided for the suspension.
     * @return A {@link Mono} emitting the persisted {@link HashAudit} record.
     */
    private Mono<HashAudit> createAuditLog(HashToken token, String executor, String reason) {
        return hashAuditRepository.save(HashAudit.create(
                token.tenantId(),
                token.id(),
                "HASH_DEACTIVATION",
                "SUCCESS",
                executor,
                Map.of(
                        "reason", reason,
                        "tokenId", token.id(),
                        "previousStatus", "ACTIVE",
                        "newStatus", token.status().name()
                )
        ));
    }
}