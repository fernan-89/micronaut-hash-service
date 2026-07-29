package com.thinklab.application.interactor;

import com.thinklab.application.port.out.HashAuditRepositoryPort;
import com.thinklab.application.port.out.HashTokenRepositoryPort;
import com.thinklab.application.usecase.command.ReactivateHashCommand;
import com.thinklab.domain.exception.HashNotFoundException;
import com.thinklab.domain.model.HashAudit;
import com.thinklab.domain.model.HashToken;
import com.thinklab.domain.valueobject.HashStatus;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 *  Unit Test: Application Interactor for Hash Token reactivation.
 *  This suite validates the state transition from INACTIVE back to ACTIVE,
 *  ensuring strict adherence to Identity Sovereignty (ADR 005) and
 *  atomic forensic auditing.
 */
@ExtendWith(MockitoExtension.class)
class ReactivateHashInteractorTest {

    @Mock
    private HashTokenRepositoryPort hashTokenRepository;

    @Mock
    private HashAuditRepositoryPort hashAuditRepository;

    @InjectMocks
    private ReactivateHashInteractor interactor;

    private UUID hashId;
    private ReactivateHashCommand command;

    /**
     * Initializes the context with deterministic UUIDs.
     */
    @BeforeEach
    void setUp() {
        hashId = UUID.randomUUID();
        // CRITICAL FIX: Pass the UUID object directly to the command.
        // ReactivateHashCommand now enforces strong typing [1, 5].
        command = new ReactivateHashCommand(
                hashId,
                "nasa-sre-operator",
                "Restoring operational capability after security validation."
        );
    }

    @Test
    @DisplayName("Should successfully reactivate token and generate forensic audit")
    void shouldReactivateAndAuditSuccessfully() {
        HashToken initialToken = mock(HashToken.class);
        HashToken reactivatedToken = mock(HashToken.class);
        HashAudit mockAudit = mock(HashAudit.class);

        // All discovery methods now use the native UUID [6, 7]
        when(hashTokenRepository.findById(hashId)).thenReturn(Mono.just(initialToken));
        when(initialToken.reactivate(command.executor())).thenReturn(reactivatedToken);

        when(reactivatedToken.id()).thenReturn(hashId);
        when(reactivatedToken.tenantId()).thenReturn("TENANT-NASA-01");

        when(hashTokenRepository.update(reactivatedToken)).thenReturn(Mono.just(reactivatedToken));
        when(hashAuditRepository.save(any(HashAudit.class))).thenReturn(Mono.just(mockAudit));

        StepVerifier.create(interactor.execute(command))
                .expectNext(reactivatedToken)
                .verifyComplete();

        verify(hashTokenRepository, times(1)).findById(hashId);
        verify(hashTokenRepository, times(1)).update(reactivatedToken);
        verify(hashAuditRepository, times(1)).save(any(HashAudit.class));
    }

    @Test
    @DisplayName("Should signal HashNotFoundException and abort when entity is missing")
    void shouldAbortPipelineWhenTokenNotFound() {
        when(hashTokenRepository.findById(hashId)).thenReturn(Mono.empty());

        StepVerifier.create(interactor.execute(command))
                .expectError(HashNotFoundException.class)
                .verify();

        verify(hashTokenRepository, never()).update(any());
        verify(hashAuditRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should fail fast with NullPointerException when the command is null")
    void shouldThrowNullPointerExceptionWhenCommandIsNull() {
        Assertions.assertThrows(NullPointerException.class, () -> interactor.execute(null));
        verifyNoInteractions(hashTokenRepository, hashAuditRepository);
    }
}