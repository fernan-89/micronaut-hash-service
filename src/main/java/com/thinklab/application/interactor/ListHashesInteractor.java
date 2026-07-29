package com.thinklab.application.interactor;

import com.thinklab.application.usecase.command.ListHashesQuery;
import com.thinklab.application.port.out.HashTokenRepositoryPort;
import com.thinklab.domain.model.HashToken;
import com.thinklab.application.port.in.ListHashesUseCase;
import io.micronaut.data.model.Pageable;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;

import java.util.Objects;

/**
 * Application Interactor: Implementation of the {@link ListHashesUseCase} input port.
 *
 * <p><b>Architectural Role:</b>
 * This interactor orchestrates the retrieval of paginated hash token registries, strictly adhering
 * to CQRS principles for read-only query streams. It guarantees multi-tenant isolation by enforcing
 * mandatory tenant filters across all persistence queries.
 *
 * <p><b>Contractual Obligations:</b>
 * <ul>
 * <li><b>Constructor Injection (ADR-001):</b> Utilizes an explicit {@link Inject} constructor to guarantee
 *     deterministic bean wiring and AOP proxy reliability.</li>
 * <li><b>Reactive Streaming (Backpressure):</b> Emits items asynchronously via Project Reactor {@link Flux},
 *     allowing downstream consumers to handle backpressure safely without stalling Netty EventLoops.</li>
 * <li><b>Fail-Fast Validation:</b> Defensively rejects null queries or invalid parameters before stream subscription.</li>
 * </ul>
 *
 * @author ThinkLab
 * @version 1.0.0
 * @since 1.0
 */
@Slf4j
@Singleton
public class ListHashesInteractor implements ListHashesUseCase {

    private final HashTokenRepositoryPort hashTokenRepository;

    /**
     * Explicit constructor for strict dependency injection (ADR-001).
     *
     * @param hashTokenRepository The outbound port for token retrieval. Must not be null.
     * @throws NullPointerException if the repository port is null.
     */
    @Inject
    public ListHashesInteractor(HashTokenRepositoryPort hashTokenRepository) {
        this.hashTokenRepository = Objects.requireNonNull(hashTokenRepository, "Application constraint violated: HashTokenRepositoryPort cannot be null.");
    }

    /**
     * Retrieves a paginated reactive stream of {@link HashToken} aggregates based on the provided query context.
     *
     * <p><b>Reactive Pipeline Flow:</b>
     * <ol>
     *   <li>Constructs Micronaut Data {@link Pageable} boundaries from zero-indexed page and size constraints.</li>
     *   <li>Branches based on the presence of an optional {@link com.thinklab.domain.valueobject.HashStatus} filter.</li>
     *   <li>Streams matching domain aggregates through the outbound repository port with structured logging hooks.</li>
     * </ol>
     *
     * @param query The {@link ListHashesQuery} encapsulating tenant context, status filter, and pagination metadata. Must not be null.
     * @return A {@link Flux} emitting matching {@link HashToken} domain aggregates.
     * @throws NullPointerException if the provided query is null.
     */
    @Override
    public Flux<HashToken> execute(ListHashesQuery query) {
        Objects.requireNonNull(query, "Application constraint violated: ListHashesQuery cannot be null.");

        Pageable pageable = Pageable.from(query.page(), query.size());
        String tenantId = query.tenantId();

        return Flux.defer(() -> {
                    log.info("[ACTION: LIST_HASHES] [TENANT: {}] [STATUS: {}] [PAGE: {}] [SIZE: {}] - Initiating paginated retrieval pipeline.",
                            tenantId, query.status(), query.page(), query.size());

                    if (query.status() != null) {
                        return hashTokenRepository.findAllByTenantIdAndStatus(tenantId, query.status(), pageable)
                                .doOnError(error -> log.error("[ACTION: LIST_HASHES] [TENANT: {}] - CRITICAL: Pipeline failed during filtered retrieval: {}",
                                        tenantId, error.getMessage(), error));
                    }

                    return hashTokenRepository.findAllByTenantId(tenantId, pageable)
                            .doOnError(error -> log.error("[ACTION: LIST_HASHES] [TENANT: {}] - CRITICAL: Pipeline failed during full retrieval: {}",
                                    tenantId, error.getMessage(), error));
                })
                .doOnComplete(() -> log.info("[ACTION: LIST_HASHES] [TENANT: {}] - Retrieval stream completed successfully.", tenantId));
    }
}