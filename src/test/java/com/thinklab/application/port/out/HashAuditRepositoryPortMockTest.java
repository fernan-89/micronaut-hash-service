package com.thinklab.application.port.out;

import com.thinklab.domain.model.HashAudit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HashAuditRepositoryPortMockTest {

    @Mock
    private HashAuditRepositoryPort repositoryPort;

    @Test
    @DisplayName("Deve simular a persistência de um HashAudit com sucesso")
    void shouldSaveAuditSuccessfully() {
        // Given
        HashAudit mockAudit = mock(HashAudit.class);
        when(repositoryPort.save(any(HashAudit.class))).thenReturn(Mono.just(mockAudit));

        // When & Then
        StepVerifier.create(repositoryPort.save(mockAudit))
                .expectNext(mockAudit)
                .verifyComplete();

        verify(repositoryPort).save(mockAudit);
    }

    @Test
    @DisplayName("Deve simular a busca de HashAudit pelo Transaction ID (txId)")
    void shouldFindAuditByTxId() {
        // Given
        String txId = "tx-123";
        HashAudit mockAudit = mock(HashAudit.class);
        when(repositoryPort.findByTxId(eq(txId))).thenReturn(Flux.just(mockAudit, mockAudit));

        // When & Then
        StepVerifier.create(repositoryPort.findByTxId(txId))
                .expectNextCount(2)
                .verifyComplete();

        verify(repositoryPort).findByTxId(txId);
    }

    @Test
    @DisplayName("Deve simular a busca de HashAudit pelo Tenant ID")
    void shouldFindAuditByTenantId() {
        // Given
        String tenantId = "tenant-456";
        HashAudit mockAudit = mock(HashAudit.class);
        when(repositoryPort.findByTenantId(eq(tenantId))).thenReturn(Flux.just(mockAudit));

        // When & Then
        StepVerifier.create(repositoryPort.findByTenantId(tenantId))
                .expectNext(mockAudit)
                .verifyComplete();

        verify(repositoryPort).findByTenantId(tenantId);
    }

    @Test
    @DisplayName("Deve simular a busca de HashAudit pelo Entity ID")
    void shouldFindAuditByEntityId() {
        // Given
        String entityId = "entity-789";
        HashAudit mockAudit = mock(HashAudit.class);
        when(repositoryPort.findByEntityId(eq(entityId))).thenReturn(Flux.just(mockAudit));

        // When & Then
        StepVerifier.create(repositoryPort.findByEntityId(entityId))
                .expectNext(mockAudit)
                .verifyComplete();

        verify(repositoryPort).findByEntityId(entityId);
    }
}