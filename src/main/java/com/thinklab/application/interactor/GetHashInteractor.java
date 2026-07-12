package com.thinklab.application.interactor;

import com.thinklab.application.command.GetHashQuery;
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
 * This service provides read-only access to the cryptographic hash registry, following
 * the CQRS principle by separating retrieval logic from state mutation operations.
 *
 * <p><b>Architectural Rules:</b></p>
 * <ul>
 *     <li><b>Non-blocking:</b> Operates strictly within the Project Reactor pipeline.</li>
 *     <li><b>Semantic Error Handling:</b> Signals {@link HashNotFoundException} if the resource is missing.</li>
 *     <li><b>Zero Side-Effects:</b> This interactor performs no auditing or state changes.</li>
 * </ul>
 */
@Slf4j
@Singleton
@RequiredArgsConstructor
public class GetHashInteractor implements GetHashUseCase {

    private final HashTokenRepositoryPort hashTokenRepository;

    @Override
    @Nonnull
    public Mono<HashToken> execute(@Nonnull GetHashQuery query) {
        Objects.requireNonNull(query, "GetHashQuery cannot be null.");

        log.debug("Executing hash retrieval for ID: [{}]", query.hashId());

        return hashTokenRepository.findById(query.hashId())
                .switchIfEmpty(Mono.defer(() -> {
                    log.warn("HashToken retrieval failed: ID [{}] not found.", query.hashId());
                    return Mono.error(new HashNotFoundException(query.hashId()));
                }))
                .doOnSuccess(token -> log.debug("HashToken successfully retrieved: [{}]", token.id()))
                .doOnError(error -> log.error("Error occurred while retrieving HashToken [{}]: {}",
                        query.hashId(), error.getMessage()));
    }
}