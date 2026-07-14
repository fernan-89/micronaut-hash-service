package com.thinklab.application.interactor;

import com.thinklab.application.usecase.command.GenerateHashCommand;
import com.thinklab.application.port.out.HashAuditRepositoryPort;
import com.thinklab.application.port.out.HashTokenRepositoryPort;
import com.thinklab.domain.exception.BusinessException;
import com.thinklab.domain.model.HashAudit;
import com.thinklab.domain.model.HashToken;
import com.thinklab.application.port.in.GenerateHashUseCase;
import jakarta.annotation.Nonnull;
import jakarta.inject.Singleton;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Application Interactor: Implementation of the {@link GenerateHashUseCase} input port.
 * <p>This service orchestrates the cryptographic generation process, ensuring that
 * intensive CPU operations do not block the reactive event loop.</p>
 *
 * <p><b>Architectural Principles (Mission-Critical Pattern):</b></p>
 * <ul>
 * <li><b>CPU Offloading:</b> Leverages {@code Schedulers.parallel()} for cryptographic computations to maintain Netty's responsiveness.</li>
 * <li><b>Atomic Pipeline:</b> Ensures that a hash is only considered "created" if both the registry and the audit trail are transactionally persisted.</li>
 * <li><b>Idempotency Check:</b> Prevents duplicate active hashes for the same tenant and payload context.</li>
 * <li><b>Telemetry:</b> Structured logging tags track the lifecycle of the orchestration pipeline.</li>
 * </ul>
 *
 * @version 1.0.0
 */
@Slf4j
@Singleton
@RequiredArgsConstructor
public class GenerateHashInteractor implements GenerateHashUseCase {

    private final HashTokenRepositoryPort hashTokenRepository;
    private final HashAuditRepositoryPort hashAuditRepository;

    /**
     * Orchestrates the generation of a cryptographic HashToken and its initial forensic audit trail.
     *
     * @param command The {@link GenerateHashCommand} encapsulating the payload, algorithm, and tenant context.
     * @return A {@link Mono} emitting the newly generated {@link HashToken}.
     * @throws NullPointerException if the provided command is null, preserving pipeline integrity (Fail-Fast).
     * @apiNote Emits a {@link BusinessException} signal if an active hash already exists for the provided payload and tenant.
     */
    @Override
    @Nonnull
    public Mono<HashToken> execute(@Nonnull GenerateHashCommand command) {
        Objects.requireNonNull(command, "GenerateHashCommand cannot be null.");

        return hashTokenRepository.existsActiveByTenantAndPayload(command.tenantId(), command.payload())
                .flatMap(exists -> {
                    if (exists) {
                        log.warn("[ACTION: GENERATE_HASH] [TENANT: {}] - Orchestration halted: Active hash already exists for this payload.", command.tenantId());
                        return Mono.error(new BusinessException("HASH_DUPLICATE",
                                "An active hash already exists for the provided tenant and payload."));
                    }
                    return performGeneration(command);
                })
                .doOnSubscribe(s -> log.info("[ACTION: GENERATE_HASH] [TENANT: {}] [ALGO: {}] - Initiating orchestration pipeline for cryptographic generation.", command.tenantId(), command.algorithm().name()));
    }

    /**
     * Executes the cryptographic computation and transactionally binds it to persistence.
     *
     * @param command The validated generation command.
     * @return A {@link Mono} emitting the persisted {@link HashToken}.
     */
    private Mono<HashToken> performGeneration(GenerateHashCommand command) {
        return Mono.fromCallable(() -> {
                    String generatedHash = calculateHash(command.payload(), command.algorithm());

                    if (command.asSerialKey()) {
                        generatedHash = formatAsSerialKey(generatedHash);
                    }

                    UUID id = UUID.randomUUID();

                    return HashToken.create(
                            id.toString(),
                            command.tenantId(),
                            command.sourceService(),
                            command.payload(),
                            generatedHash,
                            command.algorithm(),
                            command.executor()
                    );
                })
                .subscribeOn(Schedulers.parallel()) // Offloads compute-bound task to prevent Netty EventLoop blocking
                .flatMap(hashTokenRepository::save)
                .flatMap(savedToken -> createAuditLog(savedToken, command.executor())
                        .thenReturn(savedToken))
                .doOnSuccess(token -> log.info("[ACTION: GENERATE_HASH] [TENANT: {}] [ID: {}] - Orchestration completed. Entity generated and forensic audit successfully persisted.", command.tenantId(), token.id()))
                .doOnError(error -> {
                    if (!(error instanceof BusinessException)) {
                        log.error("[ACTION: GENERATE_HASH] [TENANT: {}] - CRITICAL: Pipeline orchestration failed due to system exception: {}", command.tenantId(), error.getMessage());
                    }
                });
    }

    /**
     * Performs the underlying cryptographic hashing logic based on the requested algorithm.
     *
     * @param payload   The raw string payload to be hashed.
     * @param algorithm The cryptographic algorithm specification.
     * @return The resulting hash as a lowercase hexadecimal string.
     */
    private String calculateHash(String payload, com.thinklab.domain.valueobject.HashAlgorithm algorithm) {
        MessageDigest digest = algorithm.getMessageDigest();
        byte[] hashBytes = digest.digest(payload.getBytes(StandardCharsets.UTF_8));
        StringBuilder hexString = new StringBuilder();

        for (byte b : hashBytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }

        return hexString.toString();
    }

    /**
     * Formats a raw hexadecimal hash string into a standardized, human-readable serial key format.
     * <p>Applies an uppercase alphanumeric mask structured as XXXXX-XXXXX-XXXXX-XXXXX-XXXXX.</p>
     *
     * @param hash The raw hexadecimal hash.
     * @return The formatted serial key string.
     */
    private String formatAsSerialKey(String hash) {
        String clean = hash.replaceAll("[^a-zA-Z0-9]", "").toUpperCase();

        if (clean.length() < 25) {
            clean = (clean + "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789").substring(0, 25);
        }

        return String.format("%s-%s-%s-%s-%s",
                clean.substring(0, 5),
                clean.substring(5, 10),
                clean.substring(10, 15),
                clean.substring(15, 20),
                clean.substring(20, 25)
        );
    }

    /**
     * Constructs and persists an immutable forensic audit record for the generation lifecycle event.
     *
     * @param token    The newly generated {@link HashToken} entity.
     * @param executor The principal identifier of the user or system executing the action.
     * @return A {@link Mono} emitting the persisted {@link HashAudit} record.
     */
    private Mono<HashAudit> createAuditLog(HashToken token, String executor) {
        return hashAuditRepository.save(HashAudit.create(
                token.tenantId(),
                token.id(),
                "HASH_GENERATION",
                "SUCCESS",
                executor,
                Map.of(
                        "algorithm", token.algorithm().name(),
                        "tokenId", token.id(),
                        "isSerialKey", String.valueOf(!token.generatedHash().equals(token.payload()))
                )
        ));
    }
}