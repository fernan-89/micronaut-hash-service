package com.thinklab.application.interactor;

import com.thinklab.application.usecase.command.ListHashesQuery;
import com.thinklab.application.port.out.HashTokenRepositoryPort;
import com.thinklab.domain.model.HashToken;
import com.thinklab.application.port.in.ListHashesUseCase;
import io.micronaut.data.model.Pageable;
import jakarta.annotation.Nonnull;
import jakarta.inject.Singleton;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;

import java.util.Objects;

/**
 * Application Interactor: Implementation of the {@link ListHashesUseCase} input port.
 * <p>This service orchestrates the retrieval of hash token registries using reactive
 * pagination and filtering. It adheres to the CQRS principle for read-only operations.</p>
 *
 * <p><b>Architectural Principles (Mission-Critical Pattern):</b></p>
 * <ul>
 * <li><b>Reactive Streaming:</b> Returns a {@link Flux} to support backpressure and efficient resource utilization.</li>
 * <li><b>Multi-tenancy:</b> Enforces data isolation by requiring a mandatory tenant identifier.</li>
 * <li><b>Non-blocking:</b> Fully integrated with the Project Reactor pipeline.</li>
 * <li><b>Telemetry:</b> Structured logging tags track the lifecycle of the list orchestration.</li>
 * </ul>
 *
 * @version 1.0.0
 */
@Slf4j
@Singleton
@RequiredArgsConstructor
public class ListHashesInteractor implements ListHashesUseCase {

    private final HashTokenRepositoryPort hashTokenRepository;

    /**
     * Retrieves a paginated stream of HashTokens based on the provided query context.
     *
     * @param query The {@link ListHashesQuery} encapsulating tenant context, status filter, and pagination metadata.
     * @return A {@link Flux} emitting the matching {@link HashToken} entities.
     * @throws NullPointerException if the provided query is null, preserving pipeline integrity (Fail-Fast).
     */
    @Override
    @Nonnull
    public Flux<HashToken> execute(@Nonnull ListHashesQuery query) {
        Objects.requireNonNull(query, "ListHashesQuery cannot be null.");

        Pageable pageable = Pageable.from(query.page(), query.size());

        return Flux.defer(() -> {
                    log.info("[ACTION: LIST_HASHES] [TENANT: {}] [STATUS: {}] [PAGE: {}] - Initiating paginated retrieval pipeline.",
                            query.tenantId(), query.status(), query.page());

                    if (query.status() != null) {
                        return hashTokenRepository.findAllByTenantIdAndStatus(query.tenantId(), query.status(), pageable)
                                .doOnError(error -> log.error("[ACTION: LIST_HASHES] [TENANT: {}] - CRITICAL: Pipeline failed during filtered retrieval: {}",
                                        query.tenantId(), error.getMessage()));
                    }

                    return hashTokenRepository.findAllByTenantId(query.tenantId(), pageable)
                            .doOnError(error -> log.error("[ACTION: LIST_HASHES] [TENANT: {}] - CRITICAL: Pipeline failed during full retrieval: {}",
                                    query.tenantId(), error.getMessage()));
                })
                .doOnComplete(() -> log.info("[ACTION: LIST_HASHES] [TENANT: {}] - Retrieval pipeline completed.", query.tenantId()));
    }
}