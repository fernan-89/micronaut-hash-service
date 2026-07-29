package com.thinklab.application.interactor;

import com.thinklab.application.port.out.HashTokenRepositoryPort;
import com.thinklab.application.usecase.command.ListHashesQuery;
import com.thinklab.domain.model.HashToken;
import com.thinklab.domain.valueobject.HashStatus;
import io.micronaut.data.model.Pageable;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 *  Unit Test: Application Interactor for paginated Hash Token retrieval.
 *  This suite validates the CQRS read-model routing logic, ensuring that multi-tenant
 *  isolation is enforced and that the interactor correctly dispatches queries to the
 *  appropriate outbound ports based on status filtering criteria.
 *
 *  <p><b>Architectural Principles:</b></p>
 *  <ul>
 *      <li><b>CQRS Read-Side:</b> Validates pure retrieval flows without state mutation.</li>
 *      <li><b>Tenant Isolation:</b> Ensures all queries are strictly scoped to a tenant identifier.</li>
 *      <li><b>Reactive Integrity:</b> Verified utilizing {@link StepVerifier} for non-blocking stream validation.</li>
 *  </ul>
 */
@ExtendWith(MockitoExtension.class)
class ListHashesInteractorTest {

    @Mock
    private HashTokenRepositoryPort hashTokenRepository;

    @Mock
    private ListHashesQuery query;

    @InjectMocks
    private ListHashesInteractor interactor;

    private String tenantId;

    /**
     * Initializes the testing context with deterministic multi-tenant identity.
     */
    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID().toString();
    }

    /**
     * Happy Path: Validates that when a specific status is provided, the interactor
     * routes the request to the filtered status query in the repository.
     */
    @Test
    @DisplayName("Should route to filtered find when status is provided in query")
    void shouldRouteToFilteredFindAllWhenStatusIsProvided() {
        // Given: Query contains a specific status filter
        HashStatus status = HashStatus.ACTIVE;
        int page = 0;
        int size = 10;

        when(query.tenantId()).thenReturn(tenantId);
        when(query.status()).thenReturn(status);
        when(query.page()).thenReturn(page);
        when(query.size()).thenReturn(size);

        HashToken token1 = mock(HashToken.class);
        HashToken token2 = mock(HashToken.class);

        when(hashTokenRepository.findAllByTenantIdAndStatus(eq(tenantId), eq(status), any(Pageable.class)))
                .thenReturn(Flux.just(token1, token2));

        // When & Then: Execute the stream and verify routing precision
        StepVerifier.create(interactor.execute(query))
                .expectNext(token1)
                .expectNext(token2)
                .verifyComplete();

        // Forensic validation: Filtered method must be called, global search must be skipped
        verify(hashTokenRepository, times(1)).findAllByTenantIdAndStatus(eq(tenantId), eq(status), any(Pageable.class));
        verify(hashTokenRepository, never()).findAllByTenantId(anyString(), any(Pageable.class));
    }

    /**
     * Happy Path: Validates that when status is null, the interactor routes the
     * request to the global tenant retrieval method.
     */
    @Test
    @DisplayName("Should route to global tenant find when status filter is null")
    void shouldRouteToGlobalFindAllWhenStatusIsNull() {
        // Given: Status filter is explicitly absent
        int page = 1;
        int size = 20;

        when(query.tenantId()).thenReturn(tenantId);
        when(query.status()).thenReturn(null);
        when(query.page()).thenReturn(page);
        when(query.size()).thenReturn(size);

        HashToken token = mock(HashToken.class);

        when(hashTokenRepository.findAllByTenantId(eq(tenantId), any(Pageable.class)))
                .thenReturn(Flux.just(token));

        // When & Then: Execute and verify global routing
        StepVerifier.create(interactor.execute(query))
                .expectNext(token)
                .verifyComplete();

        verify(hashTokenRepository, times(1)).findAllByTenantId(eq(tenantId), any(Pageable.class));
        verify(hashTokenRepository, never()).findAllByTenantIdAndStatus(anyString(), any(), any(Pageable.class));
    }

    /**
     * Defensive Boundary: Ensures that null query inputs are caught synchronously
     * to protect the EventLoop from illegal states.
     */
    @Test
    @DisplayName("Should fail fast with NullPointerException when the query is null")
    void shouldThrowNullPointerExceptionWhenQueryIsNull() {
        // When & Then: Synchronous assertion for fail-fast behavior
        NullPointerException exception = Assertions.assertThrows(
                NullPointerException.class,
                () -> interactor.execute(null)
        );

        Assertions.assertEquals("ListHashesQuery cannot be null.", exception.getMessage());

        // Critical: Verify absolute isolation
        verifyNoInteractions(hashTokenRepository);
    }

    /**
     * Resilience Case: Validates that infrastructure-level failures (e.g. Database Timeout)
     * are correctly propagated through the reactive stream for telemetry capture.
     */
    @Test
    @DisplayName("Should propagate reactive error when repository fails during retrieval")
    void shouldPropagateErrorWhenRepositoryFails() {
        // Given: Simulating a critical database failure
        when(query.tenantId()).thenReturn(tenantId);
        when(query.status()).thenReturn(HashStatus.ACTIVE);
        when(query.page()).thenReturn(0);
        when(query.size()).thenReturn(5);

        RuntimeException dbException = new RuntimeException("MongoDB connection lost");
        when(hashTokenRepository.findAllByTenantIdAndStatus(anyString(), any(), any(Pageable.class)))
                .thenReturn(Flux.error(dbException));

        // When & Then: Verify error propagation
        StepVerifier.create(interactor.execute(query))
                .expectErrorMatches(throwable -> throwable.getMessage().equals("MongoDB connection lost"))
                .verify();

        verify(hashTokenRepository).findAllByTenantIdAndStatus(anyString(), any(), any(Pageable.class));
    }
}