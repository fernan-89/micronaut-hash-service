package com.thinklab.application.interactor;

import com.thinklab.application.port.out.HashTokenRepositoryPort;
import com.thinklab.application.usecase.command.GetHashQuery;
import com.thinklab.domain.exception.HashNotFoundException;
import com.thinklab.domain.model.HashToken;
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

import static org.mockito.Mockito.*;

/**
 *  Unit Test: Application Interactor for specific Hash Token retrieval.
 *  This suite validates the read-side of the CQRS pattern, ensuring high-assurance
 *  identity discovery, multi-tenant isolation, and resilient error propagation
 *  within a non-blocking reactive pipeline.
 *
 *  <p><b>Architectural Principles:</b></p>
 *  <ul>
 *      <li><b>Identity Sovereignty:</b> Enforces native {@link UUID} compliance to match BSON Subtype 4 storage.</li>
 *      <li><b>Reactive Integrity:</b> Verified utilizing {@link StepVerifier} for non-blocking signal validation.</li>
 *      <li><b>Defensive Boundary:</b> Validates synchronous fail-fast protection and semantic domain exceptions.</li>
 *  </ul>
 */
@ExtendWith(MockitoExtension.class)
class GetHashInteractorTest {

    @Mock
    private HashTokenRepositoryPort hashTokenRepository;

    @InjectMocks
    private GetHashInteractor interactor;

    private UUID hashId;
    private GetHashQuery query;

    /**
     * Initializes the testing context with deterministic identity seeds.
     */
    @BeforeEach
    void setUp() {
        hashId = UUID.randomUUID();
        query = new GetHashQuery(hashId);
    }

    /**
     * Happy Path: Validates that a valid and existent identifier results in a
     * successful entity projection through the reactive stream.
     */
    @Test
    @DisplayName("Should successfully return HashToken when located by the repository")
    void shouldReturnHashTokenSuccessfully() {
        // Given: Repository contains the target aggregate
        HashToken mockToken = mock(HashToken.class);
        when(mockToken.id()).thenReturn(hashId);
        when(hashTokenRepository.findById(hashId)).thenReturn(Mono.just(mockToken));

        // When & Then: Execute pipeline and verify emission
        StepVerifier.create(interactor.execute(query))
                .expectNext(mockToken)
                .verifyComplete();

        // Forensic validation of execution sequence
        verify(hashTokenRepository, times(1)).findById(hashId);
    }

    /**
     * Business Invariant: Validates that non-existent entities result in a semantic
     * HashNotFoundException signal, preventing null-leakage into the web adapter.
     */
    @Test
    @DisplayName("Should emit HashNotFoundException signal when the entity does not exist")
    void shouldEmitHashNotFoundExceptionWhenEntityDoesNotExist() {
        // Given: Repository returns empty for the provided UUID
        when(hashTokenRepository.findById(hashId)).thenReturn(Mono.empty());

        // When & Then: Expect terminal error signal with domain semantics
        StepVerifier.create(interactor.execute(query))
                .expectError(HashNotFoundException.class)
                .verify();

        verify(hashTokenRepository, times(1)).findById(hashId);
    }

    /**
     * Defensive Boundary: Ensures that null query inputs are caught synchronously
     * at the application edge to prevent resource allocation in the EventLoop.
     */
    @Test
    @DisplayName("Should fail fast with NullPointerException when the query is null")
    void shouldThrowNullPointerExceptionWhenQueryIsNull() {
        // When & Then: Validate synchronous boundary defense matching application guard pattern
        NullPointerException exception = Assertions.assertThrows(
                NullPointerException.class,
                () -> interactor.execute(null)
        );

        Assertions.assertEquals("Application constraint violated: GetHashQuery cannot be null.", exception.getMessage());

        // Verify total isolation: Zero infrastructure interaction on malformed input
        verifyNoInteractions(hashTokenRepository);
    }

    /**
     * Resilience Case: Validates that critical infrastructure failures (e.g. Database Timeout)
     * are correctly propagated and logged via critical telemetry.
     */
    @Test
    @DisplayName("Should propagate infrastructure exceptions and trigger critical telemetry")
    void shouldPropagateSystemExceptionWhenRepositoryFails() {
        // Given: A critical system failure simulation
        RuntimeException internalDbError = new RuntimeException("MongoDB Cluster Connection Timeout");
        when(hashTokenRepository.findById(hashId)).thenReturn(Mono.error(internalDbError));

        // When & Then: Verify the original exception is preserved for SRE troubleshooting
        StepVerifier.create(interactor.execute(query))
                .expectErrorMatches(throwable ->
                        throwable instanceof RuntimeException &&
                                throwable.getMessage().equals("MongoDB Cluster Connection Timeout")
                )
                .verify();

        verify(hashTokenRepository, times(1)).findById(hashId);
    }
}