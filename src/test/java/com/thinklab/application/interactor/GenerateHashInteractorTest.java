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
        command = new GenerateHashCommand(
                "tenant-123",
                "test-payload",
                "service-alpha",
                HashAlgorithm.SHA_256,
                false,
                "admin-user"
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