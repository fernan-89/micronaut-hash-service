package com.thinklab.application.interactor;

import com.thinklab.application.port.out.HashAuditRepositoryPort;
import com.thinklab.application.port.out.HashTokenRepositoryPort;
import com.thinklab.application.usecase.command.ReactivateHashCommand;
import com.thinklab.domain.exception.HashNotFoundException;
import com.thinklab.domain.model.HashAudit;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReactivateHashInteractorTest {

    @Mock
    private HashTokenRepositoryPort hashTokenRepository;

    @Mock
    private HashAuditRepositoryPort hashAuditRepository;

    @InjectMocks
    private ReactivateHashInteractor interactor;

    @Test
    @DisplayName("Deve reativar o token, persistir no banco e gerar trilha forense com sucesso")
    void shouldReactivateAndAuditSuccessfully() {
        // Given
        String hashId = "token-uuid-nasa";
        String actor = "sre-operator-01";
        String reason = "Emergency disaster recovery fallback activation";

        ReactivateHashCommand command = new ReactivateHashCommand(hashId, actor, reason);

        HashToken initialToken = mock(HashToken.class);
        HashToken reactivatedToken = mock(HashToken.class);
        HashAudit mockAudit = mock(HashAudit.class);

        // Dublagem de comportamento da máquina de estado do Domínio
        when(initialToken.reactivate(actor)).thenReturn(reactivatedToken);

        // Dublagem de comportamento de dados do Domínio para preenchimento da Auditoria
        when(reactivatedToken.id()).thenReturn(hashId);
        when(reactivatedToken.tenantId()).thenReturn("tenant-global-core");

        // Configuração das respostas sequenciais do ecossistema reativo
        when(hashTokenRepository.findById(hashId)).thenReturn(Mono.just(initialToken));
        when(hashTokenRepository.update(reactivatedToken)).thenReturn(Mono.just(reactivatedToken));
        when(hashAuditRepository.save(any(HashAudit.class))).thenReturn(Mono.just(mockAudit));

        // When & Then
        StepVerifier.create(interactor.execute(command))
                .expectNext(reactivatedToken)
                .verifyComplete();

        // Verificações Estritas de Ordem e Execução
        verify(hashTokenRepository, times(1)).findById(hashId);
        verify(initialToken, times(1)).reactivate(actor);
        verify(hashTokenRepository, times(1)).update(reactivatedToken);
        verify(hashAuditRepository, times(1)).save(any(HashAudit.class));
    }

    @Test
    @DisplayName("Deve emitir HashNotFoundException e abortar pipeline quando a entidade não existir")
    void shouldAbortPipelineWhenTokenNotFound() {
        // Given
        String hashId = "missing-token-id";
        ReactivateHashCommand command = new ReactivateHashCommand(hashId, "admin", "Testing scenario");

        when(hashTokenRepository.findById(hashId)).thenReturn(Mono.empty());

        // When & Then
        StepVerifier.create(interactor.execute(command))
                .expectError(HashNotFoundException.class)
                .verify();

        // Isolamento de Integridade: Se não achou, não pode mutar o estado nem auditar
        verify(hashTokenRepository, times(1)).findById(hashId);
        verify(hashTokenRepository, never()).update(any());
        verify(hashAuditRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve lançar NullPointerException síncrona imediatamente se o comando for nulo")
    void shouldThrowNullPointerExceptionWhenCommandIsNull() {
        // When & Then
        NullPointerException exception = Assertions.assertThrows(
                NullPointerException.class,
                () -> interactor.execute(null)
        );

        Assertions.assertEquals("ReactivateHashCommand cannot be null.", exception.getMessage());
        verifyNoInteractions(hashTokenRepository);
        verifyNoInteractions(hashAuditRepository);
    }

    @Test
    @DisplayName("Deve interromper fluxo e não gerar auditoria se a persistência do token falhar")
    void shouldPropagateErrorAndSkipAuditWhenRepositoryUpdateFails() {
        // Given
        String hashId = "token-fail-update";
        ReactivateHashCommand command = new ReactivateHashCommand(hashId, "ops-user", "Re-enabling");

        HashToken initialToken = mock(HashToken.class);
        HashToken reactivatedToken = mock(HashToken.class);
        RuntimeException dbException = new RuntimeException("Write Concern Error: MongoDB ReplicaSet Lost");

        when(initialToken.reactivate(anyString())).thenReturn(reactivatedToken);
        when(hashTokenRepository.findById(hashId)).thenReturn(Mono.just(initialToken));

        // O banco falha no meio do caminho reativo
        when(hashTokenRepository.update(reactivatedToken)).thenReturn(Mono.error(dbException));

        // When & Then
        StepVerifier.create(interactor.execute(command))
                .expectErrorMatches(throwable ->
                        throwable instanceof RuntimeException &&
                                throwable.getMessage().equals("Write Concern Error: MongoDB ReplicaSet Lost")
                )
                .verify();

        verify(hashTokenRepository, times(1)).findById(hashId);
        verify(hashTokenRepository, times(1)).update(reactivatedToken);

        // Garante que a auditoria de SUCESSO nunca foi disparada, pois o estado não foi consolidado
        verifyNoInteractions(hashAuditRepository);
    }

    @Test
    @DisplayName("Deve propagar erro crítico se o repositório de auditoria falhar na gravação do registro forense")
    void shouldPropagateErrorWhenAuditRepositoryFails() {
        // Given
        String hashId = "token-fail-audit";
        ReactivateHashCommand command = new ReactivateHashCommand(hashId, "ops-user", "Fixing status");

        HashToken initialToken = mock(HashToken.class);
        HashToken reactivatedToken = mock(HashToken.class);
        RuntimeException auditException = new RuntimeException("ElasticSearch / MongoDB Audit Cluster Down");

        when(initialToken.reactivate(anyString())).thenReturn(reactivatedToken);
        when(reactivatedToken.id()).thenReturn(hashId);
        when(reactivatedToken.tenantId()).thenReturn("tenant-error");

        when(hashTokenRepository.findById(hashId)).thenReturn(Mono.just(initialToken));
        when(hashTokenRepository.update(reactivatedToken)).thenReturn(Mono.just(reactivatedToken));

        // O update passa, mas o log de auditoria forense falha
        when(hashAuditRepository.save(any(HashAudit.class))).thenReturn(Mono.error(auditException));

        // When & Then
        StepVerifier.create(interactor.execute(command))
                .expectErrorMatches(throwable ->
                        throwable instanceof RuntimeException &&
                                throwable.getMessage().equals("ElasticSearch / MongoDB Audit Cluster Down")
                )
                .verify();

        verify(hashTokenRepository, times(1)).findById(hashId);
        verify(hashTokenRepository, times(1)).update(reactivatedToken);
        verify(hashAuditRepository, times(1)).save(any(HashAudit.class));
    }
}