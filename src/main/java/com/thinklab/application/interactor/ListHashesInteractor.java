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
 * This service orchestrates the retrieval of hash token registries using reactive
 * pagination and filtering. It adheres to the CQRS principle for read-only operations.
 *
 * <p><b>Execution Principles:</b></p>
 * <ul>
 *     <li><b>Reactive Streaming:</b> Returns a {@link Flux} to support backpressure and
 *         efficient resource utilization.</li>
 *     <li><b>Multi-tenancy:</b> Enforces data isolation by requiring a tenant identifier.</li>
 *     <li><b>Non-blocking:</b> Fully integrated with the Project Reactor pipeline.</li>
 * </ul>
 */
@Slf4j
@Singleton
@RequiredArgsConstructor
public class ListHashesInteractor implements ListHashesUseCase {

    private final HashTokenRepositoryPort hashTokenRepository;

    @Override
    @Nonnull
    public Flux<HashToken> execute(@Nonnull ListHashesQuery query) {
        Objects.requireNonNull(query, "ListHashesQuery cannot be null.");

        log.debug("Executing paginated hash listing for tenant: [{}] - Page: [{}], Size: [{}]",
                query.tenantId(), query.page(), query.size());

        // Constructing the pagination metadata for the repository layer
        var pageable = Pageable.from(query.page(), query.size());

        // Delegating to the output port with optional status filtering
        if (query.status() != null) {
            return hashTokenRepository.findAllByTenantIdAndStatus(query.tenantId(), query.status(), pageable)
                    .doOnError(error -> log.error("Failed to list hashes by status for tenant [{}]: {}",
                            query.tenantId(), error.getMessage()));
        }

        return hashTokenRepository.findAllByTenantId(query.tenantId(), pageable)
                .doOnError(error -> log.error("Failed to list all hashes for tenant [{}]: {}",
                        query.tenantId(), error.getMessage()));
    }
}