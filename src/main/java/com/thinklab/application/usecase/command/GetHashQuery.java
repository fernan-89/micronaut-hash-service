package com.thinklab.application.usecase.command;

import io.micronaut.core.annotation.Introspected;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;

import java.util.Objects;
import java.util.UUID;

/**
 * Application Query: Encapsulates the criteria for retrieving a specific cryptographic {@link com.thinklab.domain.model.HashToken}.
 *
 * <p><b>Architectural Role:</b>
 * This immutable query record serves as the formal request payload for read-only retrieval operations.
 * Adhering strictly to CQRS principles, it ensures input validation at the application boundary, leveraging
 * native UUID representation to guarantee database indexing efficiency and eliminate injection risks.
 *
 * <p><b>Contractual Obligations:</b>
 * <ul>
 * <li><b>Immutability:</b> Implemented as a Java record to guarantee thread-safe query propagation across reactive streams.</li>
 * <li><b>Identity Sovereignty:</b> Enforces native {@link UUID} representation for target identifiers, aligning
 *     with BSON Binary (Subtype 4) performance standards.</li>
 * <li><b>CQRS Segregation:</b> Exclusively supports read-side query operations, completely isolated from state-mutating commands.</li>
 * <li><b>Defensive Sanitization:</b> Compact constructor executes fail-fast validation checks with structured logging hooks.</li>
 * </ul>
 *
 * @param hashId The universally unique identifier (UUID) of the HashToken to be retrieved. Must not be null.
 *
 * @author ThinkLab
 * @version 1.0.0
 * @since 1.0
 */
@Slf4j
@Introspected
public record GetHashQuery(
        @NotNull(message = "Hash ID UUID is mandatory for retrieval")
        UUID hashId
) {

    /**
     * Compact constructor for defensive programming and structured forensic logging.
     * Acts as the final gatekeeper for data integrity, intercepting invalid queries immediately.
     */
    public GetHashQuery {
        Objects.requireNonNull(hashId, "Application constraint violated: hashId UUID cannot be null.");
    }
}