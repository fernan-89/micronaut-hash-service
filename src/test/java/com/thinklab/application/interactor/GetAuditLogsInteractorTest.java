package com.thinklab.application.interactor;

import com.thinklab.application.port.out.HashAuditRepositoryPort;
import com.thinklab.domain.model.HashAudit;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GetAuditLogsInteractorTest {

    @Mock
    private HashAuditRepositoryPort auditRepository;

    @InjectMocks
    private GetAuditLogsInteractor interactor;

    @Test
    @DisplayName("Deve retornar Flux contendo os logs de auditoria quando entityId for válido")
    void shouldReturnAuditLogsSuccessfully() {
        // Given
        String entityId = "entity-uuid-123";
        HashAudit log1 = mock(HashAudit.class);
        HashAudit log2 = mock(HashAudit.class);

        when(auditRepository.findByEntityId(entityId)).thenReturn(Flux.just(log1, log2));

        // When & Then
        StepVerifier.create(interactor.execute(entityId))
                .expectNext(log1)
                .expectNext(log2)
                .verifyComplete();

        verify(auditRepository, times(1)).findByEntityId(entityId);
    }

    @Test
    @DisplayName("Deve completar o Flux com sucesso e vazio quando nenhum log for encontrado")
    void shouldCompleteSuccessfullyWhenNoLogsFound() {
        // Given
        String entityId = "entity-without-logs";
        when(auditRepository.findByEntityId(entityId)).thenReturn(Flux.empty());

        // When & Then
        StepVerifier.create(interactor.execute(entityId))
                .verifyComplete(); // Garante que a pipeline fecha sem emitir nada

        verify(auditRepository, times(1)).findByEntityId(entityId);
    }

    @Test
    @DisplayName("Deve lançar NullPointerException síncrona imediatamente quando o entityId for nulo")
    void shouldThrowNullPointerExceptionWhenEntityIdIsNull() {
        // Given
        String entityId = null;

        // When & Then
        // Como o Objects.requireNonNull executa de forma síncrona fora da pipeline deferida,
        // validamos usando a asserção padrão do JUnit 5.
        NullPointerException exception = Assertions.assertThrows(
                NullPointerException.class,
                () -> interactor.execute(entityId)
        );

        Assertions.assertEquals("Entity ID cannot be null.", exception.getMessage());
        verifyNoInteractions(auditRepository); // Garante isolamento total do banco
    }

    @Test
    @DisplayName("Deve emitir erro reativo IllegalArgumentException quando o entityId for vazio ou em branco")
    void shouldEmitReactiveErrorWhenEntityIdIsBlank() {
        // Given
        String entityId = "   "; // String com espaços em branco

        // When & Then
        // Como a validação do isBlank está dentro do Flux.defer(), ela gera um sinal de erro reativo.
        StepVerifier.create(interactor.execute(entityId))
                .expectError(IllegalArgumentException.class)
                .verify();

        verifyNoInteractions(auditRepository);
    }

    @Test
    @DisplayName("Deve propagar erro reativo e executar telemetria quando o repositório falhar")
    void shouldPropagateErrorWhenRepositoryFails() {
        // Given
        String entityId = "entity-critical-fail";
        RuntimeException databaseException = new RuntimeException("MongoDB Timeout Exception");

        when(auditRepository.findByEntityId(entityId)).thenReturn(Flux.error(databaseException));

        // When & Then
        StepVerifier.create(interactor.execute(entityId))
                .expectErrorMatches(throwable ->
                        throwable instanceof RuntimeException &&
                                throwable.getMessage().equals("MongoDB Timeout Exception")
                )
                .verify();

        verify(auditRepository, times(1)).findByEntityId(entityId);
    }
}