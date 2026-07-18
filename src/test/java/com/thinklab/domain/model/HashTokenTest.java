package com.thinklab.domain.model;

import com.thinklab.domain.valueobject.HashAlgorithm;
import com.thinklab.domain.valueobject.HashStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class HashTokenTest {

    @Test
    @DisplayName("Deve instanciar um HashToken com sucesso através da Factory e inicializar atributos")
    void shouldCreateHashTokenSuccessfully() {
        // Given
        String id = "token-123";
        String tenantId = "tenant-1";
        String sourceService = "auth-service";
        String payload = "my-secret-data";
        String generatedHash = "abc123hash";
        HashAlgorithm algorithm = HashAlgorithm.SHA_256; // Assumindo enum SHA_256 baseado nos testes anteriores
        String creator = "system-admin";

        // When
        HashToken token = HashToken.create(
                id, tenantId, sourceService, payload, generatedHash, algorithm, creator
        );

        // Then
        assertNotNull(token);
        assertEquals(id, token.id());
        assertEquals(tenantId, token.tenantId());
        assertEquals(HashStatus.ACTIVE, token.status(), "Um novo token deve ser criado sempre como ACTIVE");
        assertEquals(creator, token.createdBy());
        assertEquals(0L, token.version(), "A versão inicial deve ser 0");
        assertNotNull(token.createdAt());
        assertNull(token.updatedBy(), "Um token recém-criado não deve ter updatedBy");
        assertNull(token.updatedAt(), "Um token recém-criado não deve ter updatedAt");
    }

    @Test
    @DisplayName("Deve realizar transição de ACTIVE para INACTIVE (deactivate) de forma imutável")
    void shouldDeactivateTokenImmutably() {
        // Given
        HashToken token = HashToken.create(
                "token-123", "tenant-1", "service", "payload", "hash", HashAlgorithm.SHA_256, "creator"
        );

        // When
        String executor = "admin-user";
        HashToken deactivatedToken = token.deactivate(executor);

        // Then
        assertNotSame(token, deactivatedToken, "Deve retornar uma nova instância (Imutabilidade)");
        assertEquals(HashStatus.INACTIVE, deactivatedToken.status());
        assertEquals(executor, deactivatedToken.updatedBy());
        assertNotNull(deactivatedToken.updatedAt());
        // O restante deve ser mantido
        assertEquals(token.id(), deactivatedToken.id());
        assertEquals(token.version(), deactivatedToken.version());
    }

    @Test
    @DisplayName("Deve realizar transição de INACTIVE para ACTIVE (reactivate) de forma imutável")
    void shouldReactivateTokenImmutably() {
        // Given
        HashToken initialToken = HashToken.create(
                "token-123", "tenant-1", "service", "payload", "hash", HashAlgorithm.SHA_256, "creator"
        );
        HashToken deactivatedToken = initialToken.deactivate("admin-1");

        // When
        String executor = "admin-2";
        HashToken reactivatedToken = deactivatedToken.reactivate(executor);

        // Then
        assertEquals(HashStatus.ACTIVE, reactivatedToken.status());
        assertEquals(executor, reactivatedToken.updatedBy());
        assertNotNull(reactivatedToken.updatedAt());
        assertTrue(reactivatedToken.updatedAt().isAfter(deactivatedToken.updatedAt())
                || reactivatedToken.updatedAt().equals(deactivatedToken.updatedAt()));
    }

    @Test
    @DisplayName("Deve realizar transição para estado terminal REVOKED de forma imutável")
    void shouldRevokeTokenImmutably() {
        // Given
        HashToken token = HashToken.create(
                "token-123", "tenant-1", "service", "payload", "hash", HashAlgorithm.SHA_256, "creator"
        );

        // When
        String executor = "security-admin";
        HashToken revokedToken = token.revoke(executor);

        // Then
        assertEquals(HashStatus.REVOKED, revokedToken.status());
        assertEquals(executor, revokedToken.updatedBy());
        assertNotNull(revokedToken.updatedAt());
    }

    @Test
    @DisplayName("Construtor deve lançar IllegalArgumentException se strings obrigatórias estiverem em branco")
    void shouldThrowExceptionWhenStringFieldsAreBlank() {
        // Tenant em branco
        assertThrows(IllegalArgumentException.class, () -> new HashToken(
                "id", "   ", "service", "payload", "hash", HashAlgorithm.SHA_256,
                HashStatus.ACTIVE, "creator", Instant.now(), null, null, 0L
        ), "Tenant ID cannot be blank.");

        // Serviço em branco
        assertThrows(IllegalArgumentException.class, () -> new HashToken(
                "id", "tenant", "", "payload", "hash", HashAlgorithm.SHA_256,
                HashStatus.ACTIVE, "creator", Instant.now(), null, null, 0L
        ), "Source service cannot be blank.");

        // Hash em branco
        assertThrows(IllegalArgumentException.class, () -> new HashToken(
                "id", "tenant", "service", "payload", "  ", HashAlgorithm.SHA_256,
                HashStatus.ACTIVE, "creator", Instant.now(), null, null, 0L
        ), "Generated hash cannot be blank.");
    }

    @Test
    @DisplayName("Construtor deve lançar NullPointerException se atributos obrigatórios forem nulos")
    void shouldThrowExceptionWhenRequiredFieldsAreNull() {
        // Testando apenas um atributo nulo (id) para garantir o Objects.requireNonNull
        assertThrows(NullPointerException.class, () -> new HashToken(
                null, "tenant", "service", "payload", "hash", HashAlgorithm.SHA_256,
                HashStatus.ACTIVE, "creator", Instant.now(), null, null, 0L
        ), "ID cannot be null.");

        // Testando algorithm nulo
        assertThrows(NullPointerException.class, () -> new HashToken(
                "id", "tenant", "service", "payload", "hash", null,
                HashStatus.ACTIVE, "creator", Instant.now(), null, null, 0L
        ), "Algorithm cannot be null.");
    }
}