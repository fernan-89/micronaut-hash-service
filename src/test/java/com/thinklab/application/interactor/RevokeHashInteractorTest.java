package com.thinklab.application.interactor;

import com.thinklab.application.port.out.HashAuditRepositoryPort;
import com.thinklab.application.port.out.HashTokenRepositoryPort;
import com.thinklab.application.usecase.command.RevokeHashCommand;
import com.thinklab.domain.exception.HashNotFoundException;
import com.thinklab.domain.model.HashAudit;
import com.thinklab.domain.model.HashToken;
import com.thinklab.domain.valueobject.HashStatus;
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
class RevokeHashInteractorTest {

    @Mock
    private HashTokenRepositoryPort hashTokenRepository;

    @Mock
    private HashAuditRepositoryPort hashAuditRepository;

    @InjectMocks
    private RevokeHashInteractor interactor;

    @Test
    @DisplayName("Deve revogar o token permanentemente, persistir a mutação e gerar trilha de auditoria forense com sucesso")
    void shouldRevokeAndAuditSuccessfully() {
        // Given
        String hashId = "security-compromised-uuid";
        String actor = "secops-officer-99";
        String reason = "Private key leak detected in external consumer client logs";

        RevokeHashCommand command = new RevokeHashCommand(hashId, actor, reason);

        HashToken initialToken = mock(HashToken.class);
        HashToken revokedToken = mock(HashToken.class);
        HashAudit mockAudit = mock(HashAudit.class);
        HashStatus mockStatus = mock(HashStatus.class);

        // Dublagem da máquina de estado do Domínio
        when(initialToken.revoke(actor)).thenReturn(revokedToken);

        // Dublagem dos dados necessários para a composição do Log Forense
        when(revokedToken.id()).thenReturn(hashId);
        when(revokedToken.tenantId()).thenReturn("tenant-critical-financial");
        when(revokedToken.status()).thenReturn(mockStatus);
        when(mockStatus.name()).thenReturn("REVOKED");

        // Configuração das expectativas reativas das portas de saída de infraestrutura
        when(hashTokenRepository.findById(hashId)).thenReturn(Mono.just(initialToken));
        when(hashTokenRepository.update(revokedToken)).thenReturn(Mono.just(revokedToken));
        when(hashAuditRepository.save(any(HashAudit.class))).thenReturn(Mono.just(mockAudit));

        // When & Then
        StepVerifier.create(interactor.execute(command))
                .expectNext(revokedToken)
                .verifyComplete();

        // Verificações de Comportamento e Ordem de Mutação
        verify(hashTokenRepository, times(1)).findById(hashId);
        verify(initialToken, times(1)).revoke(actor);
        verify(hashTokenRepository, times(1)).update(revokedToken);
        verify(hashAuditRepository, times(1)).save(any(HashAudit.class));
    }

    @Test
    @DisplayName("Deve disparar HashNotFoundException e abortar pipeline reativa imediatamente se o token não existir")
    void shouldAbortPipelineWhenTokenNotFound() {
        // Given
        String hashId = "ghost-token-id";
        RevokeHashCommand command = new RevokeHashCommand(hashId, "secops-user", "Compromised asset lookup");

        when(hashTokenRepository.findById(hashId)).thenReturn(Mono.empty());

        // When & Then
        StepVerifier.create(interactor.execute(command))
                .expectError(HashNotFoundException.class)
                .verify();

        // Garantia de Isolamento: Se o token não existe, nenhuma persistência de mutação ou auditoria pode acontecer
        verify(hashTokenRepository, times(1)).findById(hashId);
        verify(hashTokenRepository, never()).update(any());
        verify(hashAuditRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve lançar NullPointerException síncrona imediatamente se o comando de revogação for nulo")
    void shouldThrowNullPointerExceptionWhenCommandIsNull() {
        // When & Then
        NullPointerException exception = Assertions.assertThrows(
                NullPointerException.class,
                () -> interactor.execute(null)
        );

        Assertions.assertEquals("RevokeHashCommand cannot be null.", exception.getMessage());
        verifyNoInteractions(hashTokenRepository);
        verifyNoInteractions(hashAuditRepository);
    }

    @Test
    @DisplayName("Deve propagar erro e bloquear a criação da auditoria se a persistência da atualização de status do token quebrar")
    void shouldPropagateErrorAndSkipAuditWhenTokenUpdateFails() {
        // Given
        String hashId = "token-fail-db-update";
        RevokeHashCommand command = new RevokeHashCommand(hashId, "system-monitor", "Automated rotation failure");

        HashToken initialToken = mock(HashToken.class);
        HashToken revokedToken = mock(HashToken.class);
        RuntimeException databaseException = new RuntimeException("MongoDB Network Timeout: Replica Set election in progress");

        when(initialToken.revoke(anyString())).thenReturn(revokedToken);
        when(hashTokenRepository.findById(hashId)).thenReturn(Mono.just(initialToken));

        // Simula uma falha crítica ao tentar salvar o estado REVOKED no banco
        when(hashTokenRepository.update(revokedToken)).thenReturn(Mono.error(databaseException));

        // When & Then
        StepVerifier.create(interactor.execute(command))
                .expectErrorMatches(throwable ->
                        throwable instanceof RuntimeException &&
                                throwable.getMessage().equals("MongoDB Network Timeout: Replica Set election in progress")
                )
                .verify();

        verify(hashTokenRepository, times(1)).findById(hashId);
        verify(hashTokenRepository, times(1)).update(revokedToken);

        // Defesa Arquitetural: Não podemos gerar auditoria de SUCESSO se a mutação do banco de dados faliu
        verifyNoInteractions(hashAuditRepository);
    }

    @Test
    @DisplayName("Deve estourar erro crítico se o repositório de auditoria falhar ao registrar o evento de revogação permanente")
    void shouldPropagateErrorWhenAuditLogPersistenceFails() {
        // Given
        String hashId = "token-fail-audit-log";
        RevokeHashCommand command = new RevokeHashCommand(hashId, "admin-actor", "Manual deprovisioning");

        HashToken initialToken = mock(HashToken.class);
        HashToken revokedToken = mock(HashToken.class);
        HashStatus mockStatus = mock(HashStatus.class);
        RuntimeException auditException = new RuntimeException("Audit cluster down: Kafka broker / Elasticsearch partition full");

        when(initialToken.revoke(anyString())).thenReturn(revokedToken);
        when(revokedToken.id()).thenReturn(hashId);
        when(revokedToken.tenantId()).thenReturn("tenant-core");
        when(revokedToken.status()).thenReturn(mockStatus);
        when(mockStatus.name()).thenReturn("REVOKED");

        when(hashTokenRepository.findById(hashId)).thenReturn(Mono.just(initialToken));
        when(hashTokenRepository.update(revokedToken)).thenReturn(Mono.just(revokedToken));

        // Simula a quebra do banco/fila exclusivo de registros de auditoria e compliance
        when(hashAuditRepository.save(any(HashAudit.class))).thenReturn(Mono.error(auditException));

        // When & Then
        StepVerifier.create(interactor.execute(command))
                .expectErrorMatches(throwable ->
                        throwable instanceof RuntimeException &&
                                throwable.getMessage().equals("Audit cluster down: Kafka broker / Elasticsearch partition full")
                )
                .verify();

        verify(hashTokenRepository, times(1)).findById(hashId);
        verify(hashTokenRepository, times(1)).update(revokedToken);
        verify(hashAuditRepository, times(1)).save(any(HashAudit.class));
    }
}