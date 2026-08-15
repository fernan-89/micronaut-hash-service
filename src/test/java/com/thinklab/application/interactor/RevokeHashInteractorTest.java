package com.thinklab.application.interactor;

import com.thinklab.application.port.out.HashAuditRepositoryPort;
import com.thinklab.application.port.out.HashTokenRepositoryPort;
import com.thinklab.application.usecase.command.RevokeHashCommand;
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
 *  Unit Test: Application Interactor for Terminal Hash Revocation.
 *  This suite validates the irreversible revocation lifecycle, ensuring that
 *  identity sovereignty (UUID) is preserved and forensic audit trails are
 *  atomically persisted within a non-blocking reactive pipeline.
 *
 *  <p><b>Principles:</b></p>
 *  <ul>
 *      <li><b>Identity Sovereignty (ADR 005):</b> Native UUID enforcement.</li>
 *      <li><b>Zero Trust:</b> Mandatory justification for terminal state transitions.</li>
 *      <li><b>Reactive Integrity:</b> Verified using Project Reactor StepVerifier.</li>
 *  </ul>
 */
@ExtendWith(MockitoExtension.class)
class RevokeHashInteractorTest {

    @Mock
    private HashTokenRepositoryPort hashTokenRepository;

    @Mock
    private HashAuditRepositoryPort hashAuditRepository;

    @InjectMocks
    private RevokeHashInteractor interactor;

    private UUID hashId;
    private RevokeHashCommand command;

    /**
     * Initializes the context with deterministic UUIDs.
     */
    @BeforeEach
    void setUp() {
        hashId = UUID.randomUUID();
        command = new RevokeHashCommand(
                hashId,
                "secops-officer-99",
                "Private key leak detected in external consumer client logs"
        );
    }

    /**
     * Happy Path: Validates successful revocation and forensic audit persistence.
     */
    @Test
    @DisplayName("Should successfully revoke token and persist forensic audit trail")
    void shouldRevokeAndAuditSuccessfully() {
        // Given: Aggregate exists and domain logic returns a revoked instance
        HashToken initialToken = mock(HashToken.class);
        HashToken revokedToken = mock(HashToken.class);
        HashAudit mockAudit = mock(HashAudit.class);

        when(hashTokenRepository.findById(hashId)).thenReturn(Mono.just(initialToken));
        when(initialToken.revoke(command.executor())).thenReturn(revokedToken);

        when(revokedToken.id()).thenReturn(hashId);
        when(revokedToken.status()).thenReturn(HashStatus.REVOKED);
        when(revokedToken.tenantId()).thenReturn("TENANT-CRITICAL-01");

        when(hashTokenRepository.update(revokedToken)).thenReturn(Mono.just(revokedToken));
        when(hashAuditRepository.save(any(HashAudit.class))).thenReturn(Mono.just(mockAudit));

        // When & Then: Execute reactive pipeline
        StepVerifier.create(interactor.execute(command))
                .expectNext(revokedToken)
                .verifyComplete();

        // Forensic verification
        verify(hashTokenRepository, times(1)).findById(hashId);
        verify(hashTokenRepository, times(1)).update(revokedToken);
        verify(hashAuditRepository, times(1)).save(any(HashAudit.class));
    }

    /**
     * Business Invariant: Ensures the pipeline aborts if the entity is not found.
     */
    @Test
    @DisplayName("Should signal HashNotFoundException when the registry does not exist")
    void shouldAbortPipelineWhenTokenNotFound() {
        // Given: Repository returns empty for the native UUID
        when(hashTokenRepository.findById(hashId)).thenReturn(Mono.empty());

        // When & Then: Terminal error signal expected
        StepVerifier.create(interactor.execute(command))
                .expectError(HashNotFoundException.class)
                .verify();

        // Integrity Check: No mutation or audit must occur
        verify(hashTokenRepository, never()).update(any());
        verify(hashAuditRepository, never()).save(any());
    }

    /**
     * Defensive Boundary: Validates synchronous fail-fast for null inputs with uniform constraint messages.
     */
    @Test
    @DisplayName("Should fail fast with NullPointerException when the command is null")
    void shouldThrowNullPointerExceptionWhenCommandIsNull() {
        NullPointerException exception = Assertions.assertThrows(
                NullPointerException.class,
                () -> interactor.execute(null)
        );

        Assertions.assertEquals("Application constraint violated: RevokeHashCommand cannot be null.", exception.getMessage());
        verifyNoInteractions(hashTokenRepository, hashAuditRepository);
    }
}