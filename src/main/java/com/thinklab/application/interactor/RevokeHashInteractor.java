package com.thinklab.application.interactor;

import com.thinklab.application.usecase.command.RevokeHashCommand;
import com.thinklab.application.port.out.HashAuditRepositoryPort;
import com.thinklab.application.port.out.HashTokenRepositoryPort;
import com.thinklab.domain.exception.HashNotFoundException;
import com.thinklab.domain.model.HashAudit;
import com.thinklab.domain.model.HashToken;
import com.thinklab.application.port.in.RevokeHashUseCase;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Application Interactor: Implementation of the {@link RevokeHashUseCase} input port.
 *
 * <p><b>Architectural Role:</b>
 * This interactor orchestrates the permanent and irreversible revocation of a cryptographic hash token.
 * Following Zero Trust and secure forensic principles, terminal revocation mandates an explicit business
 * justification and transactional auditing for strict security compliance.
 *
 * <p><b>Contractual Obligations:</b>
 * <ul>
 * <li><b>Constructor Injection (ADR-001):</b> Utilizes an explicit {@link Inject} constructor to guarantee
 *     deterministic bean wiring and AOP proxy reliability.</li>
 * <li><b>Identity Sovereignty:</b> Enforces native {@link UUID} parameters to maintain strict
 *     BSON Binary (Subtype 4) performance optimization across database boundaries.</li>
 * <li><b>Terminal State Transition:</b> Drives the domain aggregate into a permanent, immutable REVOKED status.</li>
 * <li><b>Atomic Audit Binding:</b> Every successful termination transactionalizes the generation and
 *     persistence of an immutable {@link HashAudit} forensic event correlated by a unique transaction UUID.</li>
 * </ul>
 *
 * @author ThinkLab
 * @version 1.0.0
 * @since 1.0
 */
@Slf4j
@Singleton
public class RevokeHashInteractor implements RevokeHashUseCase {

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
    public RevokeHashInteractor(
            HashTokenRepositoryPort hashTokenRepository,
            HashAuditRepositoryPort hashAuditRepository
    ) {
        this.hashTokenRepository = Objects.requireNonNull(hashTokenRepository, "Application constraint violated: HashTokenRepositoryPort cannot be null.");
        this.hashAuditRepository = Objects.requireNonNull(hashAuditRepository, "Application constraint violated: HashAuditRepositoryPort cannot be null.");
    }

    /**
     * Orchestrates the irreversible revocation of a {@link HashToken} and ensures the transactional generation
     * of an immutable forensic audit trail.
     *
     * <p><b>Reactive Pipeline Flow:</b>
     * <ol>
     *   <li>Fetches the token by its UUID (Emits {@link HashNotFoundException} via {@code switchIfEmpty} if absent).</li>
     *   <li>Applies the pure domain termination via {@code revoke(executor)}, enforcing terminal state invariants.</li>
     *   <li>Persists the updated aggregate via the repository port.</li>
     *   <li>Generates and persists the forensic audit log correlated by a reactive transaction UUID.</li>
     * </ol>
     *
     * @param command The {@link RevokeHashCommand} encapsulating the target UUID, executor, and justification. Must not be null.
     * @return A {@link Mono} emitting the mutated {@link HashToken} in its terminal REVOKED state.
     * @throws NullPointerException if the provided command is null.
     */
    @Override
    public Mono<HashToken> execute(RevokeHashCommand command) {
        Objects.requireNonNull(command, "Application constraint violated: RevokeHashCommand cannot be null.");

        UUID hashId = command.hashId();
        UUID txId = UUID.randomUUID(); // Reactive transaction correlation ID for auditing

        return hashTokenRepository.findById(hashId)
                .switchIfEmpty(Mono.defer(() -> {
                    log.warn("[ACTION: REVOKE_HASH] [ID: {}] [TX: {}] - Orchestration halted: Entity not found in system of record.", hashId, txId);
                    return Mono.error(new HashNotFoundException(hashId));
                }))
                .map(existingToken -> existingToken.revoke(command.executor()))
                .flatMap(hashTokenRepository::update)
                .flatMap(revokedToken -> createAuditLog(revokedToken, txId, command.executor(), command.reason())
                        .thenReturn(revokedToken))
                .doOnSubscribe(s -> log.warn("[ACTION: REVOKE_HASH] [ID: {}] [TX: {}] [EXECUTOR: {}] - CRITICAL: Initiating orchestration pipeline for permanent entity revocation.", hashId, txId, command.executor()))
                .doOnSuccess(token -> log.warn("[ACTION: REVOKE_HASH] [ID: {}] [TX: {}] - CRITICAL: Orchestration successfully completed. Entity permanently revoked and forensic audit committed.", hashId, txId))
                .doOnError(error -> {
                    if (!(error instanceof HashNotFoundException)) {
                        log.error("[ACTION: REVOKE_HASH] [ID: {}] [TX: {}] - CRITICAL: Pipeline orchestration failed due to system exception: {}", hashId, txId, error.getMessage(), error);
                    }
                });
    }

    /**
     * Constructs and persists an immutable forensic audit record for the terminal revocation lifecycle event.
     *
     * @param token    The newly revoked {@link HashToken} entity.
     * @param txId     The reactive transaction correlation UUID.
     * @param executor The principal identifier of the user or system executing the action.
     * @param reason   The business justification provided for the permanent revocation.
     * @return A {@link Mono} emitting the successfully persisted {@link HashAudit} record.
     */
    private Mono<HashAudit> createAuditLog(HashToken token, UUID txId, String executor, String reason) {
        HashAudit audit = HashAudit.create(
                txId,
                token.tenantId(),
                token.id(),
                "HASH_REVOCATION",
                "SUCCESS",
                executor,
                Map.of(
                        "reason", reason,
                        "tokenId", token.id().toString(),
                        "terminalAction", "TRUE",
                        "finalStatus", token.status().name()
                )
        );

        return hashAuditRepository.save(audit);
    }
}