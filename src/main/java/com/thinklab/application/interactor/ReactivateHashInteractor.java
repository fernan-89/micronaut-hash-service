package com.thinklab.application.interactor;

import com.thinklab.application.usecase.command.ReactivateHashCommand;
import com.thinklab.application.port.out.HashAuditRepositoryPort;
import com.thinklab.application.port.out.HashTokenRepositoryPort;
import com.thinklab.domain.exception.HashNotFoundException;
import com.thinklab.domain.model.HashAudit;
import com.thinklab.domain.model.HashToken;
import com.thinklab.application.port.in.ReactivateHashUseCase;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Application Interactor: Implementation of the {@link ReactivateHashUseCase} input port.
 *
 * <p><b>Architectural Role:</b>
 * This interactor orchestrates the business process for restoring an {@link com.thinklab.domain.valueobject.HashStatus#INACTIVE}
 * cryptographic hash back to an ACTIVE operational status. It guarantees that the state transition
 * is strictly validated by the domain state machine and transactionally bound to an immutable forensic audit trail.
 *
 * <p><b>Contractual Obligations:</b>
 * <ul>
 * <li><b>Constructor Injection (ADR-001):</b> Utilizes an explicit {@link Inject} constructor to guarantee
 *     deterministic bean wiring and AOP proxy reliability.</li>
 * <li><b>Identity Sovereignty:</b> Enforces native {@link UUID} parameters to maintain strict
 *     BSON Binary (Subtype 4) performance optimization across database boundaries.</li>
 * <li><b>Atomic Audit Binding:</b> Every successful state transition transactionalizes the generation and
 *     persistence of an immutable {@link HashAudit} forensic event correlated by a unique transaction UUID.</li>
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
public class ReactivateHashInteractor implements ReactivateHashUseCase {

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
    public ReactivateHashInteractor(
            HashTokenRepositoryPort hashTokenRepository,
            HashAuditRepositoryPort hashAuditRepository
    ) {
        this.hashTokenRepository = Objects.requireNonNull(hashTokenRepository, "Application constraint violated: HashTokenRepositoryPort cannot be null.");
        this.hashAuditRepository = Objects.requireNonNull(hashAuditRepository, "Application constraint violated: HashAuditRepositoryPort cannot be null.");
    }

    /**
     * Executes the secure reactivation of an existing {@link HashToken}.
     *
     * <p><b>Reactive Pipeline Flow:</b>
     * <ol>
     *   <li>Fetches the token by its UUID (Emits {@link HashNotFoundException} via {@code switchIfEmpty} if absent).</li>
     *   <li>Applies the pure domain state mutation via {@code reactivate(executor)}, enforcing state machine rules.</li>
     *   <li>Persists the updated aggregate via the repository port.</li>
     *   <li>Generates and persists the forensic audit log correlated by a reactive transaction UUID.</li>
     * </ol>
     *
     * @param command The {@link ReactivateHashCommand} containing the target UUID, executor, and justification. Must not be null.
     * @return A {@link Mono} emitting the successfully reactivated {@link HashToken} in its new ACTIVE state.
     * @throws NullPointerException if the provided command is null.
     */
    @Override
    public Mono<HashToken> execute(ReactivateHashCommand command) {
        Objects.requireNonNull(command, "Application constraint violated: ReactivateHashCommand cannot be null.");

        UUID hashId = command.hashId();
        UUID txId = UUID.randomUUID(); // Reactive transaction correlation ID for auditing

        return Mono.defer(() -> {
            log.info("[ACTION: REACTIVATE_HASH] [ID: {}] [TX: {}] - Initiating reactivation pipeline by actor [{}].",
                    hashId, txId, command.executor());

            return hashTokenRepository.findById(hashId)
                    .switchIfEmpty(Mono.defer(() -> {
                        log.warn("[ACTION: REACTIVATE_HASH] [ID: {}] [TX: {}] - Pipeline halted: Entity not found in system of record.", hashId, txId);
                        return Mono.error(new HashNotFoundException(hashId));
                    }))
                    .map(existingToken -> existingToken.reactivate(command.executor()))
                    .flatMap(hashTokenRepository::update)
                    .flatMap(updatedToken -> createAuditLog(updatedToken, txId, command.executor(), command.reason())
                            .thenReturn(updatedToken))
                    .doOnSuccess(token -> log.info("[ACTION: REACTIVATE_HASH] [ID: {}] [TX: {}] - HashToken successfully reactivated and audited.", token.id(), txId))
                    .doOnError(error -> {
                        if (!(error instanceof HashNotFoundException)) {
                            log.error("[ACTION: REACTIVATE_HASH] [ID: {}] [TX: {}] - CRITICAL: Pipeline orchestration failed: {}",
                                    hashId, txId, error.getMessage(), error);
                        }
                    });
        });
    }

    /**
     * Constructs and persists an immutable forensic audit record for the reactivation lifecycle event.
     *
     * @param token    The newly reactivated {@link HashToken} entity.
     * @param txId     The reactive transaction correlation UUID.
     * @param executor The principal identifier of the user or system executing the action.
     * @param reason   The business justification provided for the operational restoration.
     * @return A {@link Mono} emitting the successfully persisted {@link HashAudit} record.
     */
    private Mono<HashAudit> createAuditLog(HashToken token, UUID txId, String executor, String reason) {
        HashAudit audit = HashAudit.create(
                txId,
                token.tenantId(),
                token.id(),
                "HASH_REACTIVATION",
                "SUCCESS",
                executor,
                Map.of(
                        "reason", reason,
                        "tokenId", token.id().toString(),
                        "previousStatus", "INACTIVE",
                        "newStatus", "ACTIVE"
                )
        );

        return hashAuditRepository.save(audit);
    }
}