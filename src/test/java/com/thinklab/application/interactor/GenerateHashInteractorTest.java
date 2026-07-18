package com.thinklab.application.interactor;

import com.thinklab.application.port.out.HashAuditRepositoryPort;
import com.thinklab.application.port.out.HashTokenRepositoryPort;
import com.thinklab.application.usecase.command.GenerateHashCommand;
import com.thinklab.domain.exception.BusinessException;
import com.thinklab.domain.model.HashAudit;
import com.thinklab.domain.model.HashToken;
import com.thinklab.domain.valueobject.HashAlgorithm;
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
class GenerateHashInteractorTest {

    @Mock
    private HashTokenRepositoryPort hashTokenRepository;

    @Mock
    private HashAuditRepositoryPort hashAuditRepository;

    @InjectMocks
    private GenerateHashInteractor interactor;

    private GenerateHashCommand command;

    @BeforeEach
    void setUp() {
        // Os parâmetros agora respeitam a exata ordem do record GenerateHashCommand:
        // 1. tenantId (String)
        // 2. payload (String)
        // 3. algorithm (HashAlgorithm)
        // 4. sourceService (String)
        // 5. executor (String)
        // 6. asSerialKey (Boolean)
        command = new GenerateHashCommand(
                "tenant-123",
                "test-payload",
                HashAlgorithm.SHA_256,
                "service-alpha",
                "admin-user",
                false
        );
    }

    @Test
    @DisplayName("Deve gerar hash com sucesso quando não existir duplicado")
    void shouldGenerateHashSuccessfully() {
        // Given
        when(hashTokenRepository.existsActiveByTenantAndPayload(anyString(), anyString()))
                .thenReturn(Mono.just(false));
        when(hashTokenRepository.save(any(HashToken.class)))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
        when(hashAuditRepository.save(any(HashAudit.class)))
                .thenReturn(Mono.just(mock(HashAudit.class)));

        // When & Then
        StepVerifier.create(interactor.execute(command))
                .assertNext(token -> {
                    assert token.tenantId().equals("tenant-123");
                    assert token.payload().equals("test-payload");
                    assert token.algorithm() == HashAlgorithm.SHA_256;
                })
                .verifyComplete();

        verify(hashTokenRepository).save(any(HashToken.class));
        verify(hashAuditRepository).save(any(HashAudit.class));
    }

    @Test
    @DisplayName("Deve falhar ao tentar gerar hash duplicado para mesmo tenant e payload")
    void shouldFailWhenHashAlreadyExists() {
        // Given
        when(hashTokenRepository.existsActiveByTenantAndPayload(anyString(), anyString()))
                .thenReturn(Mono.just(true));

        // When & Then
        StepVerifier.create(interactor.execute(command))
                .expectErrorMatches(throwable ->
                        throwable instanceof BusinessException &&
                                ((BusinessException) throwable).getMessage().contains("HASH_DUPLICATE"))
                .verify();

        verify(hashTokenRepository, never()).save(any());
        verify(hashAuditRepository, never()).save(any());
    }
}