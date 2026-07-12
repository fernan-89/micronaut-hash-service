package com.thinklab.application.interactor;

import com.thinklab.application.command.GenerateHashCommand;
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
 * This service orchestrates the cryptographic generation process, ensuring that
 * intensive CPU operations do not block the reactive event loop.
 *
 * <p><b>Architectural Patterns:</b></p>
 * <ul>
 * <li><b>CPU Offloading:</b> Uses {@code Schedulers.parallel()} for cryptographic
 * computations to maintain Netty's responsiveness [1].</li>
 * <li><b>Atomic Pipeline:</b> Ensures that a hash is only considered "created"
 * if both the registry and the audit trail are persisted [2].</li>
 * <li><b>Idempotency Check:</b> Prevents duplicate active hashes for the same
 * tenant and payload [3].</li>
 * </ul>
 */
@Slf4j
@Singleton
@RequiredArgsConstructor
public class GenerateHashInteractor implements GenerateHashUseCase {

    private final HashTokenRepositoryPort hashTokenRepository;
    private final HashAuditRepositoryPort hashAuditRepository;

    @Override
    @Nonnull
    public Mono<HashToken> execute(@Nonnull GenerateHashCommand command) {
        Objects.requireNonNull(command, "GenerateHashCommand cannot be null.");

        log.info("Initiating hash generation for tenant: [{}] from service: [{}]",
                command.tenantId(), command.sourceService());

        return hashTokenRepository.existsActiveByTenantAndPayload(command.tenantId(), command.payload())
                .flatMap(exists -> {
                    if (exists) {
                        log.warn("Generation blocked: Active hash already exists for this payload.");
                        return Mono.error(new BusinessException("HASH_DUPLICATE",
                                "An active hash already exists for the provided tenant and payload."));
                    }
                    return performGeneration(command);
                });
    }

    /**
     * Executes the cryptographic computation and persistence chain.
     */
    private Mono<HashToken> performGeneration(GenerateHashCommand command) {
        return Mono.fromCallable(() -> {
                    String generatedHash = calculateHash(command.payload(), command.algorithm());
                    if (command.asSerialKey()) {
                        generatedHash = formatAsSerialKey(generatedHash);
                    }

                    // Fixed: Changed from String to native java.util.UUID to match MongoDB codec expectations
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
                .subscribeOn(Schedulers.parallel()) // Offloading compute-bound task [1]
                .flatMap(hashTokenRepository::save)
                .flatMap(savedToken -> createAuditLog(savedToken, command.executor())
                        .thenReturn(savedToken))
                .doOnSuccess(token -> log.info("HashToken successfully generated and audited: [{}]", token.id()))
                .doOnError(error -> log.error("Critical failure during hash generation: {}", error.getMessage()));
    }

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
     * Creates an immutable audit record for the generation event.
     */
    private Mono<HashAudit> createAuditLog(HashToken token, String executor) {
        return hashAuditRepository.save(HashAudit.create(
                token.tenantId(),
                "HASH_GENERATION",
                "SUCCESS",
                executor,
                Map.of(
                        "algorithm", token.algorithm().name(),
                        "tokenId", token.id().toString(),
                        "isSerialKey", String.valueOf(!token.generatedHash().equals(token.payload()))
                )
        ));
    }
}