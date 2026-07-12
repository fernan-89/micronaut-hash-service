package com.thinklab.application.port.out;

import com.thinklab.domain.model.HashAudit;
import jakarta.annotation.Nonnull;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface HashAuditRepositoryPort {

    @Nonnull
    Mono<HashAudit> save(@Nonnull HashAudit audit);

    @Nonnull
    Flux<HashAudit> findByTxId(@Nonnull String txId);

    @Nonnull
    Flux<HashAudit> findByTenantId(@Nonnull String tenantId);

    @Nonnull
    Flux<HashAudit> findByEntityId(@Nonnull String entityId);
}