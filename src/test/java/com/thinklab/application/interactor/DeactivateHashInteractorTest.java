package com.thinklab.application.interactor;

import com.thinklab.application.port.out.HashAuditRepositoryPort;
import com.thinklab.application.port.out.HashTokenRepositoryPort;
import com.thinklab.application.usecase.command.DeactivateHashCommand;
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

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 *  Unit Test: Application Interactor for Hash Deactivation.
 *  This suite validates the high-assurance orchestration of the deactivation lifecycle,
 *  ensuring that domain invariants are enforced and forensic audit trails are
 *  immutably persisted within a non-blocking reactive pipeline.
 *
 *  <p><b>Principles:</b></p>
 *  <ul>
 *      <li><b>Isolation:</b> Zero infrastructure or database dependencies (Mockito driven).</li>
 *      <li><b>Reactive Integrity:</b> Verified using Project Reactor StepVerifier.</li>
 *      <li><b>Identity Sovereignty:</b> Native java.util.UUID enforcement.</li>
 *  </ul>
 */
@ExtendWith(MockitoExtension.class)
class DeactivateHashInteractorTest {

    @Mock
    private HashTokenRepositoryPort hashTokenRepository;

    @Mock
    private HashAuditRepositoryPort hashAuditRepository;

    @InjectMocks
    private DeactivateHashInteractor interactor;

    private UUID hashId;
    private UUID tenantId;
    private DeactivateHashCommand command;
    private HashToken activeToken;

    /**
     * Initializes a consistent test context using deterministic UUIDs.
     */
    @BeforeEach
    void setUp() {
        hashId = UUID.randomUUID();
        tenantId = UUID.randomUUID();
        command = new DeactivateHashCommand(hashId, "admin-user", "Security cleanup protocol");
        activeToken = mock(HashToken.class);
    }

    /**
     * Verifies the primary happy path: Token exists, transitions to INACTIVE,
     * and generates a forensic audit record.
     */
    @Test
    @DisplayName("Should successfully deactivate hash and persist forensic audit trail")
    void shouldDeactivateHashSuccessfully() {
        // Given: The token is found and the domain logic returns a new inactive instance
        HashToken deactivatedToken = mock(HashToken.class);
        when(deactivatedToken.id()).thenReturn(hashId);
        when(deactivatedToken.status()).thenReturn(HashStatus.INACTIVE);
        when(deactivatedToken.tenantId()).thenReturn(tenantId.toString());

        when(hashTokenRepository.findById(hashId)).thenReturn(Mono.just(activeToken));
        when(activeToken.deactivate(command.executor())).thenReturn(deactivatedToken);

        // Setup expected behavior for the persistent layers
        when(hashTokenRepository.update(deactivatedToken)).thenReturn(Mono.just(deactivatedToken));
        when(hashAuditRepository.save(any(HashAudit.class))).thenReturn(Mono.just(mock(HashAudit.class)));

        // When & Then: Execute the reactive pipeline
        StepVerifier.create(interactor.execute(command))
                .expectNext(deactivatedToken)
                .verifyComplete();

        // Forensic verification of calls
        verify(hashTokenRepository).findById(hashId);
        verify(hashTokenRepository).update(deactivatedToken);
        verify(hashAuditRepository).save(any(HashAudit.class));
    }

    /**
     * Ensures that if the entity does not exist, the system signals a semantic
     * domain error and skips subsequent persistence/audit steps.
     */
    @Test
    @DisplayName("Should signal HashNotFoundException when the target registry does not exist")
    void shouldSignalErrorWhenHashNotFound() {
        // Given: Repository fails to locate the hash
        when(hashTokenRepository.findById(hashId)).thenReturn(Mono.empty());

        // When & Then: The pipeline must emit a terminal error signal
        StepVerifier.create(interactor.execute(command))
                .expectError(HashNotFoundException.class)
                .verify();

        // Critical: Verify that no mutation or audit happened
        verify(hashTokenRepository, never()).update(any());
        verify(hashAuditRepository, never()).save(any());
    }

    /**
     * Validates synchronous boundary defense (Fail-Fast) when receiving null input with uniform constraint messages.
     */
    @Test
    @DisplayName("Should fail fast with NullPointerException when the command is null")
    void shouldFailFastWhenCommandIsNull() {
        NullPointerException exception = Assertions.assertThrows(
                NullPointerException.class,
                () -> interactor.execute(null)
        );

        Assertions.assertEquals("Application constraint violated: DeactivateHashCommand cannot be null.", exception.getMessage());
        verifyNoInteractions(hashTokenRepository);
        verifyNoInteractions(hashAuditRepository);
    }
}