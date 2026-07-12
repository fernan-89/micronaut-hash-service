package com.thinklab.application.interactor;

import com.thinklab.application.command.ReactivateHashCommand;
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
 * This service orchestrates the business process of restoring an INACTIVE hash to an
 * ACTIVE state. It ensures that the state transition is governed by the domain
 * state machine and that a mandatory audit record is created for forensic purposes.
 *
 * <p><b>Architectural Rules:</b></p>
 * <ul>
 *     <li><b>Non-blocking:</b> Fully integrated into the Project Reactor pipeline.</li>
 *     <li><b>Atomic Integrity:</b> The hash is only reactivated if the audit log is successfully persisted.</li>
 *     <li><b>Stateless:</b> Implements immutable dependency injection for thread-safety.</li>
 *     <li><b>Fail-Fast:</b> Signals a semantic exception if the resource is missing or the transition is illegal.</li>
 * </ul>
 */
@Slf4j
@Singleton
@RequiredArgsConstructor
public class ReactivateHashInteractor implements ReactivateHashUseCase {

    private final HashTokenRepositoryPort hashTokenRepository;
    private final HashAuditRepositoryPort hashAuditRepository;

    @Override
    @Nonnull
    public Mono<HashToken> execute(@Nonnull ReactivateHashCommand command) {
        Objects.requireNonNull(command, "ReactivateHashCommand cannot be null.");

        log.info("Initiating reactivation orchestration for HashToken ID: [{}]", command.hashId());

        return hashTokenRepository.findById(command.hashId())
                .switchIfEmpty(Mono.defer(() -> {
                    log.warn("Reactivation failed: HashToken [{}] not found.", command.hashId());
                    return Mono.error(new HashNotFoundException(command.hashId()));
                }))
                .map(existingToken -> existingToken.reactivate(command.executor()))
                .flatMap(hashTokenRepository::save)
                .flatMap(updatedToken -> createAuditLog(updatedToken, command.executor(), command.reason())
                        .thenReturn(updatedToken))
                .doOnSuccess(token -> log.info("HashToken successfully reactivated and audited: [{}]", token.id()))
                .doOnError(error -> log.error("Critical failure during reactivation of HashToken [{}]: {}",
                        command.hashId(), error.getMessage()));
    }

    /**
     * Creates an immutable audit record for the reactivation event.
     * Encapsulates the mandatory business reason within the audit metadata.
     */
    private Mono<HashAudit> createAuditLog(HashToken token, String executor, String reason) {
        return hashAuditRepository.save(HashAudit.create(
                token.tenantId(),
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