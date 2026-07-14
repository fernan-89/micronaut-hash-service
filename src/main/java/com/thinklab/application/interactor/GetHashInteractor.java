package com.thinklab.application.interactor;

import com.thinklab.application.usecase.command.GetHashQuery;
import com.thinklab.application.port.out.HashTokenRepositoryPort;
import com.thinklab.domain.exception.HashNotFoundException;
import com.thinklab.domain.model.HashToken;
import com.thinklab.application.port.in.GetHashUseCase;
import jakarta.annotation.Nonnull;
import jakarta.inject.Singleton;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import java.util.Objects;

/**
 * Application Interactor: Implementation of the {@link GetHashUseCase} input port.
 * <p>This service provides read-only access to the cryptographic hash registry, following
 * the CQRS principle by separating retrieval logic from state mutation operations.</p>
 *
 * <p><b>Architectural Principles (Mission-Critical Pattern):</b></p>
 * <ul>
 * <li><b>Non-blocking:</b> Operates strictly within the Project Reactor pipeline.</li>
 * <li><b>Semantic Error Handling:</b> Signals a {@link HashNotFoundException} signal through the reactive stream if the target entity does not exist.</li>
 * <li><b>Zero Side-Effects:</b> This interactor performs no auditing or state changes, keeping the query footprint clean.</li>
 * <li><b>Telemetry:</b> Structured logging tags track the lifecycle of the retrieval pipeline.</li>
 * </ul>
 *
 * @version 1.0.0
 */
@Slf4j
@Singleton
@RequiredArgsConstructor
public class GetHashInteractor implements GetHashUseCase {

    private final HashTokenRepositoryPort hashTokenRepository;

    /**
     * Executes the retrieval of a HashToken by its unique system identifier.
     *
     * @param query The {@link GetHashQuery} containing the target entity identifier.
     * @return A {@link Mono} emitting the requested {@link HashToken}.
     * @throws NullPointerException if the provided query is null, preserving pipeline integrity (Fail-Fast).
     * @apiNote Emits a {@link HashNotFoundException} signal through the reactive stream if the target entity does not exist.
     */
    @Override
    @Nonnull
    public Mono<HashToken> execute(@Nonnull GetHashQuery query) {
        Objects.requireNonNull(query, "GetHashQuery cannot be null.");

        return hashTokenRepository.findById(query.hashId())
                .switchIfEmpty(Mono.defer(() -> {
                    log.warn("[ACTION: GET_HASH] [ID: {}] - Orchestration halted: Entity not found.", query.hashId());
                    return Mono.error(new HashNotFoundException(query.hashId()));
                }))
                .doOnSubscribe(s -> log.info("[ACTION: GET_HASH] [ID: {}] - Initiating retrieval pipeline.", query.hashId()))
                .doOnSuccess(token -> log.info("[ACTION: GET_HASH] [ID: {}] - Entity retrieved successfully.", token.id()))
                .doOnError(error -> {
                    if (!(error instanceof HashNotFoundException)) {
                        log.error("[ACTION: GET_HASH] [ID: {}] - CRITICAL: Pipeline orchestration failed due to system exception: {}", query.hashId(), error.getMessage());
                    }
                });
    }
}