package com.thinklab.application.interactor;

import com.thinklab.application.usecase.command.GetHashQuery;
import com.thinklab.application.port.out.HashTokenRepositoryPort;
import com.thinklab.domain.exception.HashNotFoundException;
import com.thinklab.domain.model.HashToken;
import com.thinklab.application.port.in.GetHashUseCase;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import java.util.Objects;
import java.util.UUID;

/**
 * Application Interactor: Implementation of the {@link GetHashUseCase} input port.
 *
 * <p><b>Architectural Role:</b>
 * This interactor provides read-only access to the cryptographic hash registry, strictly following
 * the CQRS (Command Query Responsibility Segregation) pattern by separating retrieval logic from
 * state-mutating command operations.
 *
 * <p><b>Contractual Obligations:</b>
 * <ul>
 * <li><b>Constructor Injection (ADR-001):</b> Utilizes an explicit {@link Inject} constructor to guarantee
 *     deterministic bean wiring and AOP proxy reliability.</li>
 * <li><b>Identity Sovereignty:</b> Leverages native {@link UUID} identifiers to ensure BSON Binary
 *     (Subtype 4) index optimization during database lookups.</li>
 * <li><b>Semantic Error Handling:</b> Safely materializes a {@link HashNotFoundException} signal through
 *     the reactive stream via {@code switchIfEmpty()} if the target aggregate is absent.</li>
 * <li><b>Zero Side-Effects:</b> Read-only query path with zero persistence mutations or audit writes.</li>
 * </ul>
 *
 * @author ThinkLab
 * @version 1.0.0
 * @since 1.0
 */
@Slf4j
@Singleton
public class GetHashInteractor implements GetHashUseCase {

    private final HashTokenRepositoryPort hashTokenRepository;

    /**
     * Explicit constructor for strict dependency injection (ADR-001).
     *
     * @param hashTokenRepository The outbound port for token retrieval. Must not be null.
     * @throws NullPointerException if the repository port is null.
     */
    @Inject
    public GetHashInteractor(HashTokenRepositoryPort hashTokenRepository) {
        this.hashTokenRepository = Objects.requireNonNull(hashTokenRepository, "Application constraint violated: HashTokenRepositoryPort cannot be null.");
    }

    /**
     * Executes the secure retrieval of a {@link HashToken} by its unique BSON-compliant UUID identifier.
     *
     * <p><b>Reactive Pipeline Flow:</b>
     * <ol>
     *   <li>Queries the repository port for the token entity by UUID.</li>
     *   <li>Intercepts empty signals with {@code switchIfEmpty()} to materialize a {@link HashNotFoundException}.</li>
     *   <li>Triggers structured telemetry hooks for subscription, success, and error outcomes.</li>
     * </ol>
     *
     * @param query The {@link GetHashQuery} encapsulating the target UUID. Must not be null.
     * @return A {@link Mono} emitting the requested {@link HashToken}.
     * @throws NullPointerException if the provided query is null.
     */
    @Override
    public Mono<HashToken> execute(GetHashQuery query) {
        Objects.requireNonNull(query, "Application constraint violated: GetHashQuery cannot be null.");

        UUID hashId = query.hashId();

        return hashTokenRepository.findById(hashId)
                .switchIfEmpty(Mono.defer(() -> {
                    log.warn("[ACTION: GET_HASH] [ID: {}] - Orchestration halted: Entity not found in system of record.", hashId);
                    return Mono.error(new HashNotFoundException(hashId));
                }))
                .doOnSubscribe(s -> log.info("[ACTION: GET_HASH] [ID: {}] - Initiating retrieval pipeline.", hashId))
                .doOnSuccess(token -> log.info("[ACTION: GET_HASH] [ID: {}] - Entity retrieved successfully.", token.id()))
                .doOnError(error -> {
                    if (!(error instanceof HashNotFoundException)) {
                        log.error("[ACTION: GET_HASH] [ID: {}] - CRITICAL: Pipeline orchestration failed due to system exception: {}", hashId, error.getMessage(), error);
                    }
                });
    }
}