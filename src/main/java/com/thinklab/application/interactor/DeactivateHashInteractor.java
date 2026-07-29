package com.thinklab.application.interactor;

import com.thinklab.application.usecase.command.DeactivateHashCommand;
import com.thinklab.application.port.out.HashAuditRepositoryPort;
import com.thinklab.application.port.out.HashTokenRepositoryPort;
import com.thinklab.domain.exception.HashNotFoundException;
import com.thinklab.domain.model.HashAudit;
import com.thinklab.domain.model.HashToken;
import com.thinklab.application.port.in.DeactivateHashUseCase;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Application Interactor: Implementation of the {@link DeactivateHashUseCase} input port.
 *
 * <p><b>Architectural Role:</b>
 * This interactor orchestrates the business process for the temporary suspension (deactivation) of a
 * cryptographic hash registry. It coordinates the core domain state machine, the reactive persistence
 * ports, and the immutable forensic audit trail required for strict compliance and security monitoring.
 *
 * <p><b>Contractual Obligations:</b>
 * <ul>
 * <li><b>Constructor Injection (ADR-001):</b> Utilizes an explicit {@link Inject} constructor to guarantee
 *     deterministic dependency wiring and AOP proxy reliability.</li>
 * <li><b>Non-Blocking I/O Pipeline:</b> 100% reactive execution using Project Reactor {@link Mono}, ensuring
 *     Netty EventLoops are never stalled by blocking threads.</li>
 * <li><b>Audit-Mandatory:</b> Every successful state transition transactionalizes the generation and
 *     persistence of an immutable {@link HashAudit} forensic event.</li>
 * <li><b>Fail-Fast Semantics:</b> Instantly rejects null commands or non-existent target entities with
 *     appropriate domain exceptions ({@link HashNotFoundException}).</li>
 * </ul>
 *
 * @author ThinkLab
 * @version 1.0.0
 * @since 1.0
 */
@Slf4j
@Singleton
public class DeactivateHashInteractor implements DeactivateHashUseCase {

    private final HashTokenRepositoryPort hashTokenRepository;
    private final HashAuditRepositoryPort hashAuditRepository;

    /**
     * Explicit constructor for strict dependency injection (ADR-001).
     *
     * @param hashTokenRepository The outbound port for token persistence. Must not be null.
     * @param hashAuditRepository The outbound port for audit persistence. Must not be null.
     * @throws NullPointerException if any repository dependency is null.
     */
    @Inject
    public DeactivateHashInteractor(
            HashTokenRepositoryPort hashTokenRepository,
            HashAuditRepositoryPort hashAuditRepository
    ) {
        this.hashTokenRepository = Objects.requireNonNull(hashTokenRepository, "Application constraint violated: HashTokenRepositoryPort cannot be null.");
        this.hashAuditRepository = Objects.requireNonNull(hashAuditRepository, "Application constraint violated: HashAuditRepositoryPort cannot be null.");
    }

    /**
     * Orchestrates the secure deactivation of a {@link HashToken} and ensures the transactional generation
     * of an immutable forensic audit trail.
     *
     * <p><b>Reactive Pipeline Flow:</b>
     * <ol>
     *   <li>Fetches the token by its UUID (Emits {@link HashNotFoundException} via {@code switchIfEmpty} if absent).</li>
     *   <li>Applies the pure domain mutation via {@code deactivate(executor)}, executing state machine checks.</li>
     *   <li>Persists the updated aggregate via the repository port.</li>
     *   <li>Generates and persists the forensic audit log containing the transaction correlation ID.</li>
     * </ol>
     *
     * @param command The {@link DeactivateHashCommand} encapsulating the target UUID, executor, and justification. Must not be null.
     * @return A {@link Mono} emitting the mutated {@link HashToken} in its new {@link com.thinklab.domain.valueobject.HashStatus#INACTIVE} state.
     * @throws NullPointerException if the provided command is null.
     */
    @Override
    public Mono<HashToken> execute(DeactivateHashCommand command) {
        Objects.requireNonNull(command, "Application constraint violated: DeactivateHashCommand cannot be null.");

        UUID hashId = command.hashId();
        UUID txId = UUID.randomUUID(); // Generate reactive transaction correlation ID for auditing

        return hashTokenRepository.findById(hashId)
                .switchIfEmpty(Mono.defer(() -> {
                    log.warn("[ACTION: DEACTIVATE_HASH] [ID: {}] [TX: {}] - Orchestration halted: Entity not found in system of record.", hashId, txId);
                    return Mono.error(new HashNotFoundException(hashId));
                }))
                .map(existingToken -> existingToken.deactivate(command.executor()))
                .flatMap(hashTokenRepository::update)
                .flatMap(updatedToken -> createAuditLog(updatedToken, txId, command.executor(), command.reason())
                        .thenReturn(updatedToken))
                .doOnSubscribe(s -> log.info("[ACTION: DEACTIVATE_HASH] [ID: {}] [TX: {}] [EXECUTOR: {}] - Initiating orchestration pipeline for status suspension.", hashId, txId, command.executor()))
                .doOnSuccess(token -> log.info("[ACTION: DEACTIVATE_HASH] [ID: {}] [TX: {}] - Orchestration successfully completed. Entity deactivated and forensic audit committed.", hashId, txId))
                .doOnError(error -> {
                    if (!(error instanceof HashNotFoundException)) {
                        log.error("[ACTION: DEACTIVATE_HASH] [ID: {}] [TX: {}] - CRITICAL: Pipeline orchestration failed due to system exception: {}", hashId, txId, error.getMessage(), error);
                    }
                });
    }

    /**
     * Constructs and persists an immutable forensic audit record for the deactivation lifecycle event.
     *
     * @param token    The newly deactivated {@link HashToken} entity.
     * @param txId     The reactive transaction correlation UUID.
     * @param executor The principal identifier of the user or system executing the action.
     * @param reason   The business justification provided for the operational suspension.
     * @return A {@link Mono} emitting the successfully persisted {@link HashAudit} record.
     */
    private Mono<HashAudit> createAuditLog(HashToken token, UUID txId, String executor, String reason) {
        HashAudit audit = HashAudit.create(
                txId,
                token.tenantId(),
                token.id(),
                "HASH_DEACTIVATION",
                "SUCCESS",
                executor,
                Map.of(
                        "reason", reason,
                        "tokenId", token.id().toString(),
                        "previousStatus", "ACTIVE",
                        "newStatus", token.status().name()
                )
        );

        return hashAuditRepository.save(audit);
    }
}