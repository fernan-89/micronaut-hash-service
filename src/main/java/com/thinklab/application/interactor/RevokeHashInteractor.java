package com.thinklab.application.interactor;

import com.thinklab.application.usecase.command.RevokeHashCommand;
import com.thinklab.application.port.out.HashAuditRepositoryPort;
import com.thinklab.application.port.out.HashTokenRepositoryPort;
import com.thinklab.domain.exception.HashNotFoundException;
import com.thinklab.domain.model.HashAudit;
import com.thinklab.domain.model.HashToken;
import com.thinklab.application.port.in.RevokeHashUseCase;
import jakarta.annotation.Nonnull;
import jakarta.inject.Singleton;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.Objects;

/**
 * Application Interactor: Implementation of the {@link RevokeHashUseCase} input port.
 * <p>This service orchestrates the permanent and irreversible revocation of a HashToken.
 * Following the Zero Trust principle, revocation requires an explicit business
 * justification and mandatory auditing for security compliance.</p>
 *
 * <p><b>Architectural Principles (Mission-Critical Pattern):</b></p>
 * <ul>
 * <li><b>Non-blocking:</b> Fully integrated into the Project Reactor pipeline.</li>
 * <li><b>Irreversible:</b> Transitions the aggregate to a terminal REVOKED state.</li>
 * <li><b>Audit-Mandatory:</b> Every revocation event transactionally triggers a forensic audit record.</li>
 * <li><b>Telemetry:</b> Structured logging tags track the lifecycle of the orchestration pipeline.</li>
 * </ul>
 *
 * @version 1.0.0
 */
@Slf4j
@Singleton
@RequiredArgsConstructor
public class RevokeHashInteractor implements RevokeHashUseCase {

    private final HashTokenRepositoryPort hashTokenRepository;
    private final HashAuditRepositoryPort hashAuditRepository;

    /**
     * Orchestrates the irreversible revocation of a HashToken and ensures the generation of an immutable audit trail.
     *
     * @param command The {@link RevokeHashCommand} encapsulating the target identifier, executor, and justification.
     * @return A {@link Mono} emitting the mutated {@link HashToken} in its terminal REVOKED state.
     * @throws NullPointerException if the provided command is null, preserving pipeline integrity (Fail-Fast).
     * @apiNote Emits a {@link HashNotFoundException} signal through the reactive stream if the target entity does not exist.
     */
    @Override
    @Nonnull
    public Mono<HashToken> execute(@Nonnull RevokeHashCommand command) {
        Objects.requireNonNull(command, "RevokeHashCommand cannot be null.");

        return hashTokenRepository.findById(command.hashId())
                .switchIfEmpty(Mono.defer(() -> {
                    log.warn("[ACTION: REVOKE_HASH] [ID: {}] - Orchestration halted: Entity not found.", command.hashId());
                    return Mono.error(new HashNotFoundException(command.hashId()));
                }))
                .map(existingToken -> existingToken.revoke(command.executor()))
                .flatMap(hashTokenRepository::update)
                .flatMap(revokedToken -> createAuditLog(revokedToken, command.executor(), command.reason())
                        .thenReturn(revokedToken))
                .doOnSubscribe(s -> log.warn("[ACTION: REVOKE_HASH] [ID: {}] [EXECUTOR: {}] - CRITICAL: Initiating orchestration pipeline for permanent entity revocation.", command.hashId(), command.executor()))
                .doOnSuccess(token -> log.warn("[ACTION: REVOKE_HASH] [ID: {}] - CRITICAL: Orchestration completed. Entity permanently revoked and forensic audit successfully persisted.", token.id()))
                .doOnError(error -> {
                    if (!(error instanceof HashNotFoundException)) {
                        log.error("[ACTION: REVOKE_HASH] [ID: {}] - CRITICAL: Pipeline orchestration failed due to system exception: {}", command.hashId(), error.getMessage());
                    }
                });
    }

    /**
     * Constructs and persists an immutable forensic audit record for the terminal revocation lifecycle event.
     *
     * @param token    The newly revoked {@link HashToken} entity.
     * @param executor The principal identifier of the user or system executing the action.
     * @param reason   The business justification provided for the permanent revocation.
     * @return A {@link Mono} emitting the persisted {@link HashAudit} record.
     */
    private Mono<HashAudit> createAuditLog(HashToken token, String executor, String reason) {
        return hashAuditRepository.save(HashAudit.create(
                token.tenantId(),
                token.id(),
                "HASH_REVOCATION",
                "SUCCESS",
                executor,
                Map.of(
                        "reason", reason,
                        "tokenId", token.id(),
                        "terminalAction", "TRUE",
                        "finalStatus", token.status().name()
                )
        ));
    }
}