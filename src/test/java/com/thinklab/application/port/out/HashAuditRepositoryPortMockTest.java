package com.thinklab.application.port.out;

import com.thinklab.domain.model.HashAudit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 *  Unit Test: Contractual validation for the {@link HashAuditRepositoryPort}.
 *  Ensures that the outbound port preserves identity sovereignty and reactive
 *  integrity when interacting with forensic audit data.
 *
 *  <p><b>Principles:</b></p>
 *  <ul>
 *      <li><b>Identity Sovereignty (ADR 005):</b> Native {@link UUID} enforcement for all identifiers.</li>
 *      <li><b>Reactive Integrity:</b> Validates signal propagation via {@link StepVerifier}.</li>
 *  </ul>
 */
@ExtendWith(MockitoExtension.class)
class HashAuditRepositoryPortMockTest {

    @Mock
    private HashAuditRepositoryPort repositoryPort;

    private UUID tenantId;
    private UUID entityId;
    private UUID txId;

    /**
     * Initializes deterministic context for each test cycle.
     */
    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        entityId = UUID.randomUUID();
        txId = UUID.randomUUID();
    }

    /**
     * Validates the persistence contract for audit records.
     */
    @Test
    @DisplayName("Should successfully simulate HashAudit persistence")
    void shouldSaveAuditSuccessfully() {
        HashAudit mockAudit = mock(HashAudit.class);
        when(repositoryPort.save(any(HashAudit.class))).thenReturn(Mono.just(mockAudit));

        StepVerifier.create(repositoryPort.save(mockAudit))
                .expectNext(mockAudit)
                .verifyComplete();

        verify(repositoryPort, times(1)).save(mockAudit);
    }

    /**
     * Validates forensic retrieval by Transaction identifier.
     */
    @Test
    @DisplayName("Should successfully retrieve audit logs correlated by Transaction UUID")
    void shouldFindAuditByTxId() {
        HashAudit log1 = mock(HashAudit.class);
        when(repositoryPort.findByTxId(txId)).thenReturn(Flux.just(log1));

        StepVerifier.create(repositoryPort.findByTxId(txId))
                .expectNext(log1)
                .verifyComplete();

        verify(repositoryPort).findByTxId(txId);
    }

    /**
     * Validates entity history reconstruction.
     * CRITICAL FIX: The port now requires a UUID parameter to match BSON optimization.
     */
    @Test
    @DisplayName("Should successfully retrieve history for a specific Entity UUID")
    void shouldFindAuditByEntityId() {
        // Given
        HashAudit mockAudit = mock(HashAudit.class);

        // CORREÇÃO: Passamos o objeto UUID diretamente sem .toString()
        when(repositoryPort.findByEntityId(entityId)).thenReturn(Flux.just(mockAudit));

        // When & Then
        StepVerifier.create(repositoryPort.findByEntityId(entityId))
                .expectNext(mockAudit)
                .verifyComplete();

        verify(repositoryPort).findByEntityId(entityId);
    }

    /**
     * Validates multi-tenant isolation.
     * Note: If findByTenantId was also updated to UUID in your port,
     * remove the .toString() below as well.
     */
    @Test
    @DisplayName("Should successfully filter forensic trails by Tenant identifier")
    void shouldFindAuditByTenantId() {
        HashAudit mockAudit = mock(HashAudit.class);
        when(repositoryPort.findByTenantId(tenantId.toString())).thenReturn(Flux.just(mockAudit));

        StepVerifier.create(repositoryPort.findByTenantId(tenantId.toString()))
                .expectNext(mockAudit)
                .verifyComplete();

        verify(repositoryPort).findByTenantId(tenantId.toString());
    }
}