package com.thinklab.application.interactor;

import com.thinklab.application.port.out.HashTokenRepositoryPort;
import com.thinklab.application.usecase.command.ListHashesQuery;
import com.thinklab.domain.model.HashToken;
import com.thinklab.domain.valueobject.HashStatus;
import io.micronaut.data.model.Pageable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ListHashesInteractorTest {

    @Mock
    private HashTokenRepositoryPort hashTokenRepository;

    @Mock
    private ListHashesQuery query;

    @InjectMocks
    private ListHashesInteractor interactor;

    @Test
    @DisplayName("Deve rotear para busca filtrada quando o status for informado na query")
    void shouldRouteToFilteredFindAllWhenStatusIsProvided() {
        // Given
        String tenantId = "tenant-nasa-1";
        HashStatus status = HashStatus.ACTIVE;
        int page = 0;
        int size = 10;

        when(query.tenantId()).thenReturn(tenantId);
        when(query.status()).thenReturn(status);
        when(query.page()).thenReturn(page);
        when(query.size()).thenReturn(size);

        HashToken token1 = mock(HashToken.class);
        HashToken token2 = mock(HashToken.class);

        // O interactor criará internamente um Pageable com esses dados
        Pageable expectedPageable = Pageable.from(page, size);

        when(hashTokenRepository.findAllByTenantIdAndStatus(eq(tenantId), eq(status), any(Pageable.class)))
                .thenReturn(Flux.just(token1, token2));

        // When & Then
        StepVerifier.create(interactor.execute(query))
                .expectNext(token1)
                .expectNext(token2)
                .verifyComplete();

        // Validações rigorosas de execução
        verify(hashTokenRepository, times(1)).findAllByTenantIdAndStatus(eq(tenantId), eq(status), any(Pageable.class));
        verify(hashTokenRepository, never()).findAllByTenantId(anyString(), any(Pageable.class));
    }

    @Test
    @DisplayName("Deve rotear para busca global por tenant quando o status não for informado")
    void shouldRouteToGlobalFindAllWhenStatusIsNull() {
        // Given
        String tenantId = "tenant-nasa-2";
        int page = 1;
        int size = 20;

        when(query.tenantId()).thenReturn(tenantId);
        when(query.status()).thenReturn(null); // Sem filtro de status
        when(query.page()).thenReturn(page);
        when(query.size()).thenReturn(size);

        HashToken token = mock(HashToken.class);

        when(hashTokenRepository.findAllByTenantId(eq(tenantId), any(Pageable.class)))
                .thenReturn(Flux.just(token));

        // When & Then
        StepVerifier.create(interactor.execute(query))
                .expectNext(token)
                .verifyComplete();

        // Garante que o método com filtro de status NUNCA foi acionado
        verify(hashTokenRepository, times(1)).findAllByTenantId(eq(tenantId), any(Pageable.class));
        verify(hashTokenRepository, never()).findAllByTenantIdAndStatus(anyString(), any(), any(Pageable.class));
    }

    @Test
    @DisplayName("Deve lançar NullPointerException imediatamente de forma síncrona se a query for nula")
    void shouldThrowNullPointerExceptionWhenQueryIsNull() {
        // When & Then
        NullPointerException exception = Assertions.assertThrows(
                NullPointerException.class,
                () -> interactor.execute(null)
        );

        Assertions.assertEquals("ListHashesQuery cannot be null.", exception.getMessage());
        verifyNoInteractions(hashTokenRepository);
    }

    @Test
    @DisplayName("Deve capturar e propagar erro reativo se o repositório falhar na busca filtrada")
    void shouldPropagateErrorWhenFilteredRepositoryFails() {
        // Given
        when(query.tenantId()).thenReturn("tenant-error");
        when(query.status()).thenReturn(HashStatus.INACTIVE);
        when(query.page()).thenReturn(0);
        when(query.size()).thenReturn(5);

        RuntimeException dbException = new RuntimeException("MongoDB connection lost");
        when(hashTokenRepository.findAllByTenantIdAndStatus(anyString(), any(), any(Pageable.class)))
                .thenReturn(Flux.error(dbException));

        // When & Then
        StepVerifier.create(interactor.execute(query))
                .expectErrorMatches(throwable ->
                        throwable instanceof RuntimeException &&
                                throwable.getMessage().equals("MongoDB connection lost")
                )
                .verify();

        verify(hashTokenRepository, times(1)).findAllByTenantIdAndStatus(anyString(), any(), any(Pageable.class));
    }

    @Test
    @DisplayName("Deve capturar e propagar erro reativo se o repositório falhar na busca global")
    void shouldPropagateErrorWhenGlobalRepositoryFails() {
        // Given
        when(query.tenantId()).thenReturn("tenant-error-global");
        when(query.status()).thenReturn(null);
        when(query.page()).thenReturn(0);
        when(query.size()).thenReturn(5);

        RuntimeException dbException = new RuntimeException("Timeout error");
        when(hashTokenRepository.findAllByTenantId(anyString(), any(Pageable.class)))
                .thenReturn(Flux.error(dbException));

        // When & Then
        StepVerifier.create(interactor.execute(query))
                .expectErrorMatches(throwable ->
                        throwable instanceof RuntimeException &&
                                throwable.getMessage().equals("Timeout error")
                )
                .verify();

        verify(hashTokenRepository, times(1)).findAllByTenantId(anyString(), any(Pageable.class));
    }
}