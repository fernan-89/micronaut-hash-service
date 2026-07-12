package com.thinklab.application.interactor;

import com.thinklab.application.command.RevokeHashCommand;
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
 * This service orchestrates the permanent and irreversible revocation of a HashToken.
 * Following the Zero Trust principle, revocation requires an explicit business
 * justification and mandatory auditing for security compliance.
 *
 * <p><b>Architectural Rules:</b></p>
 * <ul>
 *     <li><b>Non-blocking:</b> Fully integrated into the Project Reactor pipeline.</li>
 *     <li><b>Irreversible:</b> Transitions the aggregate to a terminal REVOKED state.</li>
 *     <li><b>Audit-Mandatory:</b> Every revocation event must be recorded in the audit trail.</li>
 *     <li><b>Fail-Fast:</b> Signals a semantic exception if the registry is not found.</li>
 * </ul>
 */
@Slf4j
@Singleton
@RequiredArgsConstructor
public class RevokeHashInteractor implements RevokeHashUseCase {

    private final HashTokenRepositoryPort hashTokenRepository;
    private final HashAuditRepositoryPort hashAuditRepository;

    @Override
    @Nonnull
    public Mono<HashToken> execute(@Nonnull RevokeHashCommand command) {
        Objects.requireNonNull(command, "RevokeHashCommand cannot be null.");

        log.warn("Initiating permanent revocation for HashToken ID: [{}] Authorized by: [{}]",
                command.hashId(), command.executor());

        return hashTokenRepository.findById(command.hashId())
                .switchIfEmpty(Mono.defer(() -> {
                    log.error("Revocation failed: HashToken [{}] not found.", command.hashId());
                    return Mono.error(new HashNotFoundException(command.hashId()));
                }))
                .map(existingToken -> existingToken.revoke(command.executor()))
                .flatMap(hashTokenRepository::save)
                .flatMap(revokedToken -> createAuditLog(revokedToken, command.executor(), command.reason())
                        .thenReturn(revokedToken))
                .doOnSuccess(token -> log.info("HashToken successfully revoked and audited: [{}]", token.id()))
                .doOnError(error -> log.error("Critical failure during revocation of HashToken [{}]: {}",
                        command.hashId(), error.getMessage()));
    }

    /**
     * Creates an immutable audit record for the terminal revocation event.
     * Encapsulates metadata for legal and security compliance.
     */
    private Mono<HashAudit> createAuditLog(HashToken token, String executor, String reason) {
        return hashAuditRepository.save(HashAudit.create(
                token.tenantId(),
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