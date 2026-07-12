package com.thinklab.application.interactor;

import com.thinklab.application.command.DeactivateHashCommand;
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
 * This service orchestrates the business process of deactivating a cryptographic hash registry.
 * It coordinates the interaction between the domain state machine, the persistence
 * layer, and the mandatory audit trail required for security compliance.
 *
 * <p><b>Architectural Rules:</b></p>
 * <ul>
 *     <li>Non-blocking: Operates strictly within the Project Reactor pipeline.</li>
 *     <li>Stateless: Maintains no internal state, ensuring thread-safety.</li>
 *     <li>Audit-Mandatory: Every successful deactivation triggers a forensic audit record.</li>
 *     <li>Fail-Fast: Signals a semantic exception if the identifier is missing.</li>
 * </ul>
 */
@Slf4j
@Singleton
@RequiredArgsConstructor
public class DeactivateHashInteractor implements DeactivateHashUseCase {

    private final HashTokenRepositoryPort hashTokenRepository;
    private final HashAuditRepositoryPort hashAuditRepository;

    @Override
    @Nonnull
    public Mono<HashToken> execute(@Nonnull DeactivateHashCommand command) {
        Objects.requireNonNull(command, "DeactivateHashCommand cannot be null.");

        log.debug("Initiating deactivation orchestration for HashToken ID: [{}]", command.hashId());

        return hashTokenRepository.findById(command.hashId())
                .switchIfEmpty(Mono.defer(() -> {
                    log.warn("Deactivation failed: HashToken [{}] not found.", command.hashId());
                    return Mono.error(new HashNotFoundException(command.hashId()));
                }))
                .map(existingToken -> existingToken.deactivate(command.executor()))
                .flatMap(hashTokenRepository::save)
                .flatMap(updatedToken -> createAuditLog(updatedToken, command.executor(), command.reason())
                        .thenReturn(updatedToken))
                .doOnSuccess(token -> log.info("HashToken successfully deactivated and audited: [{}]", token.id()))
                .doOnError(error -> log.error("Critical failure during deactivation of HashToken [{}]: {}",
                        command.hashId(), error.getMessage()));
    }

    /**
     * Creates an immutable audit record for the deactivation event.
     * Uses the domain factory to encapsulate log construction logic.
     */
    private Mono<HashAudit> createAuditLog(HashToken token, String executor, String reason) {
        return hashAuditRepository.save(HashAudit.create(
                token.tenantId(),
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