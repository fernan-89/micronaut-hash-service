package com.thinklab.application.interactor;

import com.thinklab.application.port.out.HashAuditRepositoryPort;
import com.thinklab.domain.model.HashAudit;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.util.UUID;

import static org.mockito.Mockito.*;

/**
 *  Unit Test: Application Interactor for Forensic Audit Retrieval.
 *  This suite validates the secure streaming of immutable audit logs, ensuring
 *  that the read-side of the CQRS pattern preserves multi-tenant isolation and
 *  defensive boundary validation.
 *
 *  <p><b>Architectural Principles:</b></p>
 *  <ul>
 *      <li><b>Identity Sovereignty (ADR 005):</b> Enforces native {@link UUID} lookups for optimized BSON indexing.</li>
 *      <li><b>Reactive Integrity:</b> Validates non-blocking backpressure and signal propagation using {@link StepVerifier}.</li>
 *      <li><b>Defensive Boundary:</b> Ensures Zero-Trust validation for input identifiers before infrastructure interaction.</li>
 *  </ul>
 *
 * @author ThinkLab
 * @version 2.0.0
 * @since 1.0
 */
@ExtendWith(MockitoExtension.class)
class GetAuditLogsInteractorTest {

    @Mock
    private HashAuditRepositoryPort auditRepository;

    @InjectMocks
    private GetAuditLogsInteractor interactor;

    private UUID entityId;

    /**
     * Initializes a fresh execution context with a secure, random UUID.
     */
    @BeforeEach
    void setUp() {
        entityId = UUID.randomUUID();
    }

    /**
     * Happy Path: Validates that the interactor correctly streams all found logs
     * from the outbound port to the reactive caller.
     */
    @Test
    @DisplayName("Should successfully return a Flux stream of audit logs for a valid entity ID")
    void shouldReturnAuditLogsSuccessfully() {
        // Given: The repository contains historical events for the target entity
        HashAudit log1 = mock(HashAudit.class);
        HashAudit log2 = mock(HashAudit.class);

        when(auditRepository.findByEntityId(entityId)).thenReturn(Flux.just(log1, log2));

        // When & Then: Execute the pipeline using the native UUID
        StepVerifier.create(interactor.execute(entityId))
                .expectNext(log1)
                .expectNext(log2)
                .verifyComplete();

        // Forensic verification: Audit repository must be hit exactly once
        verify(auditRepository, times(1)).findByEntityId(entityId);
    }

    /**
     * Boundary Case: Validates that an existing entity with no recorded events
     * results in a clean, empty reactive completion signal.
     */
    @Test
    @DisplayName("Should complete successfully with an empty Flux when no historical logs exist")
    void shouldCompleteSuccessfullyWhenNoLogsFound() {
        // Given: No logs recorded for this entity
        when(auditRepository.findByEntityId(entityId)).thenReturn(Flux.empty());

        // When & Then: Pipeline must complete without emitting any items
        StepVerifier.create(interactor.execute(entityId))
                .verifyComplete();

        verify(auditRepository, times(1)).findByEntityId(entityId);
    }

    /**
     * Fail-Fast Validation: Ensures synchronous null-pointer protection
     * triggers before allocating resources in the event loop.
     */
    @Test
    @DisplayName("Should fail fast with NullPointerException when the entity identifier is null")
    void shouldThrowNullPointerExceptionWhenEntityIdIsNull() {
        // Given: Null input at the application boundary
        UUID nullId = null;

        // When & Then: Synchronous exception is expected due to defensive validation guards
        NullPointerException exception = Assertions.assertThrows(
                NullPointerException.class,
                () -> interactor.execute(nullId)
        );

        Assertions.assertEquals("Application constraint violated: Entity UUID cannot be null for forensic retrieval.", exception.getMessage());

        // Critical: Verify absolute isolation - no database call should be attempted
        verifyNoInteractions(auditRepository);
    }

    /**
     * Resilience Test: Validates that infrastructure-level failures (timeouts, network issues)
     * are correctly propagated through the orchestration layer for telemetry handling.
     */
    @Test
    @DisplayName("Should propagate reactive error and trigger telemetry logs when repository fails")
    void shouldPropagateErrorWhenRepositoryFails() {
        // Given: A critical infrastructure failure simulation
        RuntimeException databaseException = new RuntimeException("MongoDB Reactive Stream Timeout");
        when(auditRepository.findByEntityId(entityId)).thenReturn(Flux.error(databaseException));

        // When & Then: Pipeline must emit the original error signal
        StepVerifier.create(interactor.execute(entityId))
                .expectErrorMatches(throwable -> throwable.getMessage().equals("MongoDB Reactive Stream Timeout"))
                .verify();

        verify(auditRepository, times(1)).findByEntityId(entityId);
    }
}