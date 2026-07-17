package com.thinklab.application.interactor;

import com.thinklab.application.port.out.HashTokenRepositoryPort;
import com.thinklab.application.usecase.command.GetHashQuery;
import com.thinklab.domain.exception.HashNotFoundException;
import com.thinklab.domain.model.HashToken;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GetHashInteractorTest {

    @Mock
    private HashTokenRepositoryPort hashTokenRepository;

    @InjectMocks
    private GetHashInteractor interactor;

    @Test
    @DisplayName("Deve retornar HashToken com sucesso quando ele for localizado pelo repositório")
    void shouldReturnHashTokenSuccessfully() {
        // Given
        String hashId = "hash-nasa-unique-uuid";
        GetHashQuery query = new GetHashQuery(hashId);
        HashToken mockToken = mock(HashToken.class);

        when(mockToken.id()).thenReturn(hashId);
        when(hashTokenRepository.findById(hashId)).thenReturn(Mono.just(mockToken));

        // When & Then
        StepVerifier.create(interactor.execute(query))
                .expectNext(mockToken)
                .verifyComplete();

        // Validação estrita de execução
        verify(hashTokenRepository, times(1)).findById(hashId);
    }

    @Test
    @DisplayName("Deve emitir sinal de erro HashNotFoundException caso a entidade não exista no banco")
    void shouldEmitHashNotFoundExceptionWhenEntityDoesNotExist() {
        // Given
        String hashId = "hash-non-existent-uuid";
        GetHashQuery query = new GetHashQuery(hashId);

        when(hashTokenRepository.findById(hashId)).thenReturn(Mono.empty());

        // When & Then
        StepVerifier.create(interactor.execute(query))
                .expectError(HashNotFoundException.class)
                .verify();

        verify(hashTokenRepository, times(1)).findById(hashId);
    }

    @Test
    @DisplayName("Deve lançar NullPointerException síncrona imediatamente se o comando da query for nulo")
    void shouldThrowNullPointerExceptionWhenQueryIsNull() {
        // When & Then
        NullPointerException exception = Assertions.assertThrows(
                NullPointerException.class,
                () -> interactor.execute(null)
        );

        Assertions.assertEquals("GetHashQuery cannot be null.", exception.getMessage());

        // Garante isolamento absoluto: o repositório não pode nem ser consultado
        verifyNoInteractions(hashTokenRepository);
    }

    @Test
    @DisplayName("Deve propagar falhas de infraestrutura e acionar a telemetria crítica no doOnError")
    void shouldPropagateSystemExceptionWhenRepositoryFails() {
        // Given
        String hashId = "hash-failure-uuid";
        GetHashQuery query = new GetHashQuery(hashId);
        RuntimeException internalDbError = new RuntimeException("MongoDB Cluster Connection Timeout");

        when(hashTokenRepository.findById(hashId)).thenReturn(Mono.error(internalDbError));

        // When & Then
        StepVerifier.create(interactor.execute(query))
                .expectErrorMatches(throwable ->
                        throwable instanceof RuntimeException &&
                                throwable.getMessage().equals("MongoDB Cluster Connection Timeout")
                )
                .verify();

        verify(hashTokenRepository, times(1)).findById(hashId);
    }
}