package com.thinklab.application.interactor;

import com.thinklab.application.usecase.command.ReactivateHashCommand;
import com.thinklab.application.port.out.HashAuditRepositoryPort;
import com.thinklab.application.port.out.HashTokenRepositoryPort;
import com.thinklab.domain.exception.HashNotFoundException;
import com.thinklab.domain.model.HashAudit;
import com.thinklab.domain.model.HashToken;
import com.thinklab.application.port.in.ReactivateHashUseCase;
import jakarta.annotation.Nonnull;
import jakarta.inject.Singleton;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.Objects;

/**
 * Application Interactor: Implementation of the {@link ReactivateHashUseCase} input port.
 * <p>This service orchestrates the business process of restoring an INACTIVE hash to an
 * ACTIVE state. It ensures that the state transition is governed by the domain
 * state machine and that a mandatory audit record is created for forensic purposes.</p>
 *
 * <p><b>Architectural Principles (Mission-Critical Pattern):</b></p>
 * <ul>
 * <li><b>Reactive Orchestration:</b> Fully integrated into the Project Reactor pipeline.</li>
 * <li><b>Atomic Integrity:</b> The hash state transition and the audit record persistency are linked.</li>
 * <li><b>Telemetry:</b> Structured logging tags track the lifecycle of the reactivation pipeline.</li>
 * <li><b>Fail-Fast:</b> Immediately signals illegal state transitions or missing resources.</li>
 * </ul>
 *
 * @version 1.0.0
 */
@Slf4j
@Singleton
@RequiredArgsConstructor
public class ReactivateHashInteractor implements ReactivateHashUseCase {

    private final HashTokenRepositoryPort hashTokenRepository;
    private final HashAuditRepositoryPort hashAuditRepository;

    /**
     * Executes the reactivation of an existing {@link HashToken}.
     *
     * @param command The {@link ReactivateHashCommand} containing the target identifier and executor context.
     * @return A {@link Mono} emitting the successfully reactivated {@link HashToken}.
     * @throws NullPointerException if the provided command is null (Fail-Fast).
     */
    @Override
    @Nonnull
    public Mono<HashToken> execute(@Nonnull ReactivateHashCommand command) {
        Objects.requireNonNull(command, "ReactivateHashCommand cannot be null.");

        return Mono.defer(() -> {
            log.info("[ACTION: REACTIVATE_HASH] [ID: {}] - Initiating reactivation pipeline by actor [{}].",
                    command.hashId(), command.executor());

            return hashTokenRepository.findById(command.hashId())
                    .switchIfEmpty(Mono.defer(() -> {
                        log.warn("[ACTION: REACTIVATE_HASH] [ID: {}] - Pipeline halted: Entity not found.", command.hashId());
                        return Mono.error(new HashNotFoundException(command.hashId()));
                    }))
                    .map(existingToken -> existingToken.reactivate(command.executor()))
                    .flatMap(hashTokenRepository::update)
                    .flatMap(updatedToken -> createAuditLog(updatedToken, command.executor(), command.reason())
                            .thenReturn(updatedToken))
                    .doOnSuccess(token -> log.info("[ACTION: REACTIVATE_HASH] [ID: {}] - HashToken successfully reactivated and audited.", token.id()))
                    .doOnError(error -> {
                        if (!(error instanceof HashNotFoundException)) {
                            log.error("[ACTION: REACTIVATE_HASH] [ID: {}] - CRITICAL: Pipeline orchestration failed: {}",
                                    command.hashId(), error.getMessage());
                        }
                    });
        });
    }

    /**
     * Creates an immutable audit record for the reactivation event.
     * Encapsulates the mandatory business reason within the audit metadata.
     */
    private Mono<HashAudit> createAuditLog(HashToken token, String executor, String reason) {
        return hashAuditRepository.save(HashAudit.create(
                token.tenantId(),
                token.id(),
                "HASH_REACTIVATION",
                "SUCCESS",
                executor,
                Map.of(
                        "reason", reason,
                        "tokenId", token.id(),
                        "statusTransition", "INACTIVE -> ACTIVE"
                )
        ));
    }
}