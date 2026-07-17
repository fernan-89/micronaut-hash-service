package com.thinklab.application.interactor;

import com.thinklab.application.port.out.HashAuditRepositoryPort;
import com.thinklab.application.port.out.HashTokenRepositoryPort;
import com.thinklab.application.usecase.command.DeactivateHashCommand;
import com.thinklab.domain.exception.HashNotFoundException;
import com.thinklab.domain.model.HashAudit;
import com.thinklab.domain.model.HashToken;
import com.thinklab.domain.valueobject.HashStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeactivateHashInteractorTest {

    @Mock
    private HashTokenRepositoryPort hashTokenRepository;

    @Mock
    private HashAuditRepositoryPort hashAuditRepository;

    @InjectMocks
    private DeactivateHashInteractor interactor;

    private DeactivateHashCommand command;
    private HashToken activeToken;

    @BeforeEach
    void setUp() {
        command = new DeactivateHashCommand("hash-123", "admin-user", "Security violation");
        activeToken = mock(HashToken.class);
    }

    @Test
    @DisplayName("Deve desativar o hash com sucesso e persistir auditoria")
    void shouldDeactivateHashSuccessfully() {
        // Given
        when(hashTokenRepository.findById("hash-123")).thenReturn(Mono.just(activeToken));
        when(activeToken.deactivate("admin-user")).thenReturn(activeToken);
        when(activeToken.status()).thenReturn(HashStatus.INACTIVE);
        when(activeToken.id()).thenReturn("hash-123");
        when(activeToken.tenantId()).thenReturn("tenant-1");

        when(hashTokenRepository.update(activeToken)).thenReturn(Mono.just(activeToken));
        when(hashAuditRepository.save(any(HashAudit.class))).thenReturn(Mono.just(mock(HashAudit.class)));

        // When & Then
        StepVerifier.create(interactor.execute(command))
                .expectNext(activeToken)
                .verifyComplete();

        verify(hashTokenRepository).update(activeToken);
        verify(hashAuditRepository).save(any(HashAudit.class));
    }

    @Test
    @DisplayName("Deve retornar erro quando hash não for encontrado")
    void shouldReturnErrorWhenHashNotFound() {
        // Given
        when(hashTokenRepository.findById("hash-123")).thenReturn(Mono.empty());

        // When & Then
        StepVerifier.create(interactor.execute(command))
                .expectError(HashNotFoundException.class)
                .verify();

        verify(hashTokenRepository, never()).update(any());
        verify(hashAuditRepository, never()).save(any());
    }
}