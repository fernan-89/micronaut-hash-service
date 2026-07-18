package com.thinklab.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class HashAuditTest {

    @Test
    @DisplayName("Deve instanciar HashAudit com sucesso através do método de fábrica (create)")
    void shouldCreateHashAuditViaFactory() {
        // Given
        String tenantId = "tenant-123";
        String entityId = "token-789";
        String operation = "CREATION";
        String status = "SUCCESS";
        String executorId = "admin-user";
        Map<String, Object> metadata = Map.of("ipAddress", "192.168.1.1");

        // When
        HashAudit audit = HashAudit.create(tenantId, entityId, operation, status, executorId, metadata);

        // Then
        assertNotNull(audit.id(), "O ID gerado não deve ser nulo");
        assertNotNull(audit.txId(), "O Transaction ID gerado não deve ser nulo");
        assertNotNull(audit.timestamp(), "O Timestamp gerado não deve ser nulo");

        assertEquals(tenantId, audit.tenantId());
        assertEquals(entityId, audit.entityId());
        assertEquals(operation, audit.operation());
        assertEquals(status, audit.status());
        assertEquals(executorId, audit.executorId());
        assertEquals(metadata, audit.metadata());
    }

    @Test
    @DisplayName("Deve inicializar com mapa de metadados vazio quando metadados forem nulos no método create")
    void shouldHandleNullMetadataInFactory() {
        // When
        HashAudit audit = HashAudit.create(
                "tenant-123", "token-789", "DEACTIVATION", "SUCCESS", "system", null
        );

        // Then
        assertNotNull(audit.metadata(), "A coleção de metadados não deve ser nula");
        assertTrue(audit.metadata().isEmpty(), "A coleção de metadados deve estar vazia");
    }

    @Test
    @DisplayName("Deve garantir a imutabilidade do mapa de metadados")
    void shouldEnsureMetadataIsImmutable() {
        // Given
        Map<String, Object> mutableMetadata = new HashMap<>();
        mutableMetadata.put("key1", "value1");

        HashAudit audit = HashAudit.create(
                "tenant-123", "token-789", "CREATION", "SUCCESS", "system", mutableMetadata
        );

        // When & Then
        Map<String, Object> auditMetadata = audit.metadata();

        assertThrows(UnsupportedOperationException.class, () -> {
            auditMetadata.put("key2", "value2");
        }, "Deve lançar UnsupportedOperationException ao tentar modificar metadados");
    }

    @Test
    @DisplayName("Deve lançar NullPointerException se tentar criar HashAudit com campos obrigatórios nulos no construtor")
    void shouldThrowExceptionWhenRequiredFieldsAreNull() {
        // ID nulo
        assertThrows(NullPointerException.class, () -> new HashAudit(
                null, "tx-1", "tenant-1", "entity-1", "OP", "OK", "user", Instant.now(), Map.of()
        ), "Audit ID cannot be null.");

        // Timestamp nulo
        assertThrows(NullPointerException.class, () -> new HashAudit(
                UUID.randomUUID().toString(), "tx-1", "tenant-1", "entity-1", "OP", "OK", "user", null, Map.of()
        ), "Audit timestamp is mandatory.");

        // Operation nulo
        assertThrows(NullPointerException.class, () -> new HashAudit(
                UUID.randomUUID().toString(), "tx-1", "tenant-1", "entity-1", null, "OK", "user", Instant.now(), Map.of()
        ), "Operation type is mandatory.");
    }
}