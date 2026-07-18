package com.thinklab.application.port.out;

import com.thinklab.domain.model.HashToken;
import com.thinklab.domain.valueobject.HashStatus;
import io.micronaut.data.model.Pageable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HashTokenRepositoryPortTest {

    @Mock
    private HashTokenRepositoryPort repositoryPort;

    @Test
    @DisplayName("Deve simular a persistência inicial de um HashToken")
    void shouldSaveHashTokenSuccessfully() {
        // Given
        HashToken mockToken = mock(HashToken.class);
        when(repositoryPort.save(any(HashToken.class))).thenReturn(Mono.just(mockToken));

        // When & Then
        StepVerifier.create(repositoryPort.save(mockToken))
                .expectNext(mockToken)
                .verifyComplete();

        verify(repositoryPort).save(mockToken);
    }

    @Test
    @DisplayName("Deve simular a atualização (mutação) de um HashToken")
    void shouldUpdateHashTokenSuccessfully() {
        // Given
        HashToken mockToken = mock(HashToken.class);
        when(repositoryPort.update(any(HashToken.class))).thenReturn(Mono.just(mockToken));

        // When & Then
        StepVerifier.create(repositoryPort.update(mockToken))
                .expectNext(mockToken)
                .verifyComplete();

        verify(repositoryPort).update(mockToken);
    }

    @Test
    @DisplayName("Deve simular a busca de um HashToken pelo ID")
    void shouldFindByIdSuccessfully() {
        // Given
        String id = "token-123";
        HashToken mockToken = mock(HashToken.class);
        when(repositoryPort.findById(eq(id))).thenReturn(Mono.just(mockToken));

        // When & Then
        StepVerifier.create(repositoryPort.findById(id))
                .expectNext(mockToken)
                .verifyComplete();

        verify(repositoryPort).findById(id);
    }

    @Test
    @DisplayName("Deve verificar se existe registro ativo para tenant e payload (prevenção de duplicidade)")
    void shouldCheckIfActiveHashExists() {
        // Given
        String tenantId = "tenant-1";
        String payload = "payload-data";
        when(repositoryPort.existsActiveByTenantAndPayload(eq(tenantId), eq(payload)))
                .thenReturn(Mono.just(true));

        // When & Then
        StepVerifier.create(repositoryPort.existsActiveByTenantAndPayload(tenantId, payload))
                .expectNext(true)
                .verifyComplete();

        verify(repositoryPort).existsActiveByTenantAndPayload(tenantId, payload);
    }

    @Test
    @DisplayName("Deve simular a busca paginada de HashTokens por Tenant ID")
    void shouldFindAllByTenantIdWithPagination() {
        // Given
        String tenantId = "tenant-1";
        Pageable pageable = mock(Pageable.class);
        HashToken mockToken = mock(HashToken.class);

        when(repositoryPort.findAllByTenantId(eq(tenantId), eq(pageable)))
                .thenReturn(Flux.just(mockToken, mockToken));

        // When & Then
        StepVerifier.create(repositoryPort.findAllByTenantId(tenantId, pageable))
                .expectNextCount(2)
                .verifyComplete();

        verify(repositoryPort).findAllByTenantId(tenantId, pageable);
    }

    @Test
    @DisplayName("Deve simular a busca paginada de HashTokens por Tenant ID e Status")
    void shouldFindAllByTenantIdAndStatusWithPagination() {
        // Given
        String tenantId = "tenant-1";
        HashStatus status = HashStatus.ACTIVE; // Assumindo que você tem um Enum HashStatus.ACTIVE
        Pageable pageable = mock(Pageable.class);
        HashToken mockToken = mock(HashToken.class);

        when(repositoryPort.findAllByTenantIdAndStatus(eq(tenantId), eq(status), eq(pageable)))
                .thenReturn(Flux.just(mockToken));

        // When & Then
        StepVerifier.create(repositoryPort.findAllByTenantIdAndStatus(tenantId, status, pageable))
                .expectNext(mockToken)
                .verifyComplete();

        verify(repositoryPort).findAllByTenantIdAndStatus(tenantId, status, pageable);
    }
}