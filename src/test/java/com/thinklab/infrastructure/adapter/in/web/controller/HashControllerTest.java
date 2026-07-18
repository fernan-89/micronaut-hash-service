package com.thinklab.infrastructure.adapter.in.web.controller;

import com.thinklab.application.port.in.*;
import com.thinklab.application.usecase.command.*;
import com.thinklab.domain.model.HashAudit;
import com.thinklab.domain.model.HashToken;
import com.thinklab.domain.valueobject.HashAlgorithm;
import com.thinklab.domain.valueobject.HashStatus;
import com.thinklab.infrastructure.adapter.in.web.dto.request.*;
import com.thinklab.infrastructure.adapter.in.web.dto.response.*;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HashControllerTest {

    @Mock private GenerateHashUseCase generateHashUseCase;
    @Mock private GetHashUseCase getHashUseCase;
    @Mock private ListHashesUseCase listHashesUseCase;
    @Mock private DeactivateHashUseCase deactivateHashUseCase;
    @Mock private ReactivateHashUseCase reactivateHashUseCase;
    @Mock private RevokeHashUseCase revokeHashUseCase;
    @Mock private GetAuditLogsUseCase getAuditLogsUseCase;

    @InjectMocks
    private HashController controller;

    private HashToken dummyToken;
    private final String dummyId = "9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d";

    @BeforeEach
    void setUp() {
        // Criamos um token real do domínio para evitar NullPointerExceptions
        // nos métodos estáticos "fromDomain" dos DTOs de resposta.
        dummyToken = HashToken.create(
                dummyId, "tenant-123", "service-a", "payload-data",
                "hashed-string", HashAlgorithm.SHA_256, "admin"
        );
    }

    @Test
    @DisplayName("POST /hashes - Deve delegar para GenerateHashUseCase e retornar 201 CREATED")
    void shouldGenerateHashAndReturn201() {
        // Given
        GenerateHashRequest request = mock(GenerateHashRequest.class);
        GenerateHashCommand command = mock(GenerateHashCommand.class);

        when(request.toCommand()).thenReturn(command);
        when(generateHashUseCase.execute(any(GenerateHashCommand.class)))
                .thenReturn(Mono.just(dummyToken));

        // When & Then
        StepVerifier.create(controller.generate(request))
                .assertNext(response -> {
                    assertEquals(HttpStatus.CREATED, response.getStatus());
                    assertNotNull(response.body());
                })
                .verifyComplete();

        verify(generateHashUseCase).execute(command);
    }

    @Test
    @DisplayName("GET /hashes/{id} - Deve buscar Hash por ID e retornar 200 OK")
    void shouldGetHashByIdAndReturn200() {
        // Given
        when(getHashUseCase.execute(any(GetHashQuery.class)))
                .thenReturn(Mono.just(dummyToken));

        // When & Then
        StepVerifier.create(controller.getById(dummyId))
                .assertNext(response -> {
                    assertEquals(HttpStatus.OK, response.getStatus());
                    assertNotNull(response.body());
                })
                .verifyComplete();

        verify(getHashUseCase).execute(any(GetHashQuery.class));
    }

    @Test
    @DisplayName("GET /hashes - Deve listar Hashes paginados e retornar 200 OK")
    void shouldListHashesAndReturn200() {
        // Given
        String tenantId = "tenant-123";
        HashStatus status = HashStatus.ACTIVE;

        when(listHashesUseCase.execute(any(ListHashesQuery.class)))
                .thenReturn(Flux.just(dummyToken, dummyToken));

        // When & Then
        StepVerifier.create(controller.list(tenantId, status, 0, 20))
                .assertNext(response -> {
                    assertEquals(HttpStatus.OK, response.getStatus());
                    assertNotNull(response.body());
                    assertEquals(2, response.body().content().size());
                })
                .verifyComplete();

        verify(listHashesUseCase).execute(any(ListHashesQuery.class));
    }

    @Test
    @DisplayName("PATCH /hashes/{id}/deactivate - Deve desativar e retornar 200 OK")
    void shouldDeactivateHashAndReturn200() {
        // Given
        DeactivateHashRequest request = mock(DeactivateHashRequest.class);
        DeactivateHashCommand command = mock(DeactivateHashCommand.class);

        when(request.toCommand(dummyId)).thenReturn(command);
        when(request.executor()).thenReturn("admin-user");
        when(request.reason()).thenReturn("Compromised");

        when(deactivateHashUseCase.execute(command))
                .thenReturn(Mono.just(dummyToken.deactivate("admin-user")));

        // When & Then
        StepVerifier.create(controller.deactivate(dummyId, request))
                .assertNext(response -> {
                    assertEquals(HttpStatus.OK, response.getStatus());
                    assertNotNull(response.body());
                })
                .verifyComplete();

        verify(deactivateHashUseCase).execute(command);
    }

    @Test
    @DisplayName("PATCH /hashes/{id}/reactivate - Deve reativar e retornar 200 OK")
    void shouldReactivateHashAndReturn200() {
        // Given
        ReactivateHashRequest request = mock(ReactivateHashRequest.class);
        ReactivateHashCommand command = mock(ReactivateHashCommand.class);

        // Simulamos o token inativo sendo reativado
        HashToken inactiveToken = dummyToken.deactivate("admin");

        when(request.toCommand(dummyId)).thenReturn(command);
        when(reactivateHashUseCase.execute(command))
                .thenReturn(Mono.just(inactiveToken.reactivate("admin")));

        // When & Then
        StepVerifier.create(controller.reactivate(dummyId, request))
                .assertNext(response -> {
                    assertEquals(HttpStatus.OK, response.getStatus());
                    assertNotNull(response.body());
                })
                .verifyComplete();

        verify(reactivateHashUseCase).execute(command);
    }

    @Test
    @DisplayName("DELETE /hashes/{id} - Deve revogar de forma terminal e retornar 200 OK")
    void shouldRevokeHashAndReturn200() {
        // Given
        RevokeHashRequest request = mock(RevokeHashRequest.class);
        RevokeHashCommand command = mock(RevokeHashCommand.class);

        when(request.toCommand(dummyId)).thenReturn(command);
        when(revokeHashUseCase.execute(command))
                .thenReturn(Mono.just(dummyToken.revoke("security-admin")));

        // When & Then
        StepVerifier.create(controller.revoke(dummyId, request))
                .assertNext(response -> {
                    assertEquals(HttpStatus.OK, response.getStatus());
                    assertNotNull(response.body());
                })
                .verifyComplete();

        verify(revokeHashUseCase).execute(command);
    }

    @Test
    @DisplayName("GET /hashes/{id}/audit - Deve retornar a trilha de auditoria (200 OK)")
    void shouldGetAuditTrailAndReturn200() {
        // Given
        HashAudit audit1 = HashAudit.create("tenant", dummyId, "CREATION", "SUCCESS", "admin", Map.of());
        HashAudit audit2 = HashAudit.create("tenant", dummyId, "DEACTIVATION", "SUCCESS", "admin", Map.of());

        when(getAuditLogsUseCase.execute(eq(dummyId)))
                .thenReturn(Flux.just(audit1, audit2));

        // When & Then
        StepVerifier.create(controller.getAuditTrail(dummyId))
                .assertNext(response -> {
                    assertEquals(HttpStatus.OK, response.getStatus());
                    assertNotNull(response.body());
                    assertEquals(2, response.body().size());
                })
                .verifyComplete();

        verify(getAuditLogsUseCase).execute(dummyId);
    }
}