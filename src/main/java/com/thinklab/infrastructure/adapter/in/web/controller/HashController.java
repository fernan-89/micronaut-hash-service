package com.thinklab.infrastructure.adapter.in.web.controller;

import com.thinklab.application.usecase.command.GetHashQuery;
import com.thinklab.application.usecase.command.ListHashesQuery;
import com.thinklab.application.port.in.*;
import com.thinklab.domain.valueobject.HashStatus;
import com.thinklab.infrastructure.adapter.in.web.dto.request.*;
import com.thinklab.infrastructure.adapter.in.web.dto.response.*;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.MutableHttpResponse;
import io.micronaut.http.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * REST Controller: The primary inbound adapter for the Hash Service.
 * <p>Acts as a thin mediation layer between the HTTP transport protocol and the
 * Core Application UseCases. It focuses on protocol translation, strict input
 * validation, and secure data projection to maintain domain isolation.</p>
 *
 * <p><b>Architectural Principles:</b></p>
 * <ul>
 * <li><b>Non-blocking:</b> Leverages Project Reactor for asynchronous processing.</li>
 * <li><b>Decoupled:</b> Depends only on Input Ports (Interfaces), strictly avoiding
 * exposure of internal persistence entities.</li>
 * <li><b>Defensive:</b> Enforces strict DTO validation (JSR 303/380) at the API boundary.</li>
 * </ul>
 *
 * <p><b>Telemetry & Observability (Mission-Critical Pattern):</b></p>
 * <ul>
 * <li><b>Structured Context:</b> Logs enforce a tag-based prefix strategy (e.g., [ACTION] [ENTITY]).</li>
 * <li><b>Lifecycle Tracing:</b> Streams emit logs idiomatically on Subscribe (Intent), Success (Outcome), and Error (Failure).</li>
 * <li><b>Strict Auditing:</b> Due to domain sensitivity, ALL operations (including reads) are logged at INFO level for access compliance. WARN is reserved for destructive actions (Zero Trust); ERROR for exceptions.</li>
 * </ul>
 *
 * @version 1.0.0
 */
@Slf4j
@Controller("/hashes")
@RequiredArgsConstructor
@Tag(name = "Hash Registry API", description = "Endpoints for managing cryptographic hash lifecycles, lifecycle audits, and serial keys.")
public class HashController {

    private final GenerateHashUseCase generateHashUseCase;
    private final GetHashUseCase getHashUseCase;
    private final ListHashesUseCase listHashesUseCase;
    private final DeactivateHashUseCase deactivateHashUseCase;
    private final ReactivateHashUseCase reactivateHashUseCase;
    private final RevokeHashUseCase revokeHashUseCase;
    private final GetAuditLogsUseCase getAuditLogsUseCase;

    /**
     * Generates a new cryptographic hash or serial key based on tenant specifications.
     * <p>This operation initializes the lifecycle of a cryptographic record within the system.
     * It performs cryptographic strength evaluation (where applicable) and persists the generated audit log.</p>
     *
     * @param request The {@link GenerateHashRequest} payload containing tenant metadata and hashing algorithm.
     * @return A {@link Mono} emitting a {@link MutableHttpResponse} containing the generated {@link HashResponse} record with a 210 CREATED status.
     * @throws io.micronaut.http.exceptions.HttpStatusException if payload fails strict JSR-380 validation at the framework adapter boundary.
     */
    @Post
    @Operation(
            summary = "Generate a new hash",
            description = "Calculates a cryptographic hash based on the requested algorithm, persists it securely, and registers the initial creation audit event."
    )
    @ApiResponse(responseCode = "201", description = "Hash generated, persisted, and audited successfully.")
    @ApiResponse(responseCode = "400", description = "Invalid request payload (e.g., missing required fields, empty tenant, or invalid algorithm).")
    @ApiResponse(responseCode = "500", description = "Internal server error during hash calculation or secure persistence sequence.")
    public Mono<MutableHttpResponse<HashResponse>> generate(
            @Body @Valid @Parameter(description = "Payload containing metadata and the cryptographic specification", required = true) GenerateHashRequest request
    ) {
        return generateHashUseCase.execute(request.toCommand())
                .map(HashResponse::fromDomain)
                .map(HttpResponse::created)
                .doOnSubscribe(s -> log.info("[ACTION: GENERATE_HASH] [TENANT: {}] [ALGO: {}] - Initiating entity creation protocol.", request.tenantId(), request.algorithm()))
                .doOnSuccess(res -> log.info("[ACTION: GENERATE_HASH] [TENANT: {}] - Entity creation successfully completed. Status: 201 CREATED.", request.tenantId()))
                .doOnError(err -> log.error("[ACTION: GENERATE_HASH] [TENANT: {}] - Entity creation protocol failed: {}", request.tenantId(), err.getMessage()));
    }

    /**
     * Retrieves a single hash registry record by its internal system identifier.
     * <p>Provides a sanitized project view of the target hash record, filtering out sensitive internal structures.</p>
     *
     * @param id The unique internal identifier (UUID) of the hash registry.
     * @return A {@link Mono} emitting a {@link MutableHttpResponse} with the {@link HashResponse} metadata block.
     * @throws io.micronaut.http.exceptions.HttpStatusException containing a 404 NOT FOUND status if the query returns an empty result.
     */
    @Get("/{id}")
    @Operation(
            summary = "Get hash by ID",
            description = "Retrieves the sanitized metadata, current operational state, and algorithms details of a specific hash registry."
    )
    @ApiResponse(responseCode = "200", description = "Hash record found and returned successfully.")
    @ApiResponse(responseCode = "400", description = "Malformed identifier format provided (must comply with UUID canonical standard).")
    @ApiResponse(responseCode = "404", description = "No hash record exists for the provided system identifier.")
    public Mono<MutableHttpResponse<HashResponse>> getById(
            @PathVariable @Parameter(name = "id", description = "The immutable UUID of the cryptographic hash registry", required = true, example = "9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d") String id
    ) {
        return getHashUseCase.execute(new GetHashQuery(id))
                .map(HashResponse::fromDomain)
                .map(HttpResponse::ok)
                .doOnSubscribe(s -> log.info("[ACTION: GET_HASH] [ID: {}] - Initiating secure retrieval of entity metadata and current state.", id))
                .doOnSuccess(res -> log.info("[ACTION: GET_HASH] [ID: {}] - Entity metadata successfully retrieved and projected.", id))
                .doOnError(err -> log.error("[ACTION: GET_HASH] [ID: {}] - Secure retrieval query failed: {}", id, err.getMessage()));
    }

    /**
     * Lists hashes for a specific tenant with optional status filtering and pagination.
     * <p>Performs a secure, paginated multidimensional search strictly bounded to the requested tenant environment.</p>
     *
     * @param tenantId The unique identifier of the tenant, provided via the mandatory 'X-Tenant-Id' header.
     * @param status   Optional query parameter to restrict the result list by operational state.
     * @param page     The zero-based page index. Default is 0.
     * @param size     The maximum volume of records allowed in a single page stream. Default is 20.
     * @return A {@link Mono} emitting a {@link MutableHttpResponse} containing a {@link PagedHashResponse}.
     * @throws io.micronaut.http.exceptions.HttpStatusException containing a 400 BAD REQUEST status if the tenant header is blank.
     */
    @Get
    @Operation(
            summary = "List tenant hashes",
            description = "Returns a paginated stream of hashes belonging strictly to the tenant context declared in the request header."
    )
    @ApiResponse(responseCode = "200", description = "Paginated list of hashes retrieved and projected successfully.")
    @ApiResponse(responseCode = "400", description = "Missing or blank 'X-Tenant-Id' header, or invalid pagination range parameters.")
    @ApiResponse(responseCode = "500", description = "Internal server error occurred during multitenant lookup execution.")
    public Mono<MutableHttpResponse<PagedHashResponse>> list(
            @Header("X-Tenant-Id") @NotBlank @Parameter(name = "X-Tenant-Id", description = "Unique identifier of the tenant context for multi-tenancy rules", required = true, example = "tenant-prod-alpha-1") String tenantId,
            @QueryValue @Nullable @Parameter(name = "status", description = "Optional operational state filter to scope the lookup", required = false, example = "ACTIVE") HashStatus status,
            @QueryValue(defaultValue = "0") @Parameter(name = "page", description = "Zero-based index of the target page", schema = @Schema(defaultValue = "0")) int page,
            @QueryValue(defaultValue = "20") @Parameter(name = "size", description = "The maximum volume of records to return in a single page", schema = @Schema(defaultValue = "20")) int size
    ) {
        return listHashesUseCase.execute(new ListHashesQuery(tenantId, status, page, size))
                .map(HashResponse::fromDomain)
                .collectList()
                .map(content -> PagedHashResponse.of(content, 0, page, size))
                .map(HttpResponse::ok)
                .doOnSubscribe(s -> log.info("[ACTION: LIST_HASHES] [TENANT: {}] [STATUS: {}] [PAGE: {}] [SIZE: {}] - Initiating paginated discovery query.", tenantId, status, page, size))
                .doOnSuccess(res -> log.info("[ACTION: LIST_HASHES] [TENANT: {}] - Paginated discovery completed successfully. Elements projected: {}", tenantId, res.body() != null ? res.body().content().size() : 0))
                .doOnError(err -> log.error("[ACTION: LIST_HASHES] [TENANT: {}] - Paginated discovery failed: {}", tenantId, err.getMessage()));
    }

    /**
     * Suspends a hash's operational status by transitioning it to INACTIVE.
     * <p>Deactivation is a reversible operational state change. The action requires a validated reason and context.</p>
     *
     * @param id      The internal system identifier (UUID) of the target hash.
     * @param request The {@link DeactivateHashRequest} containing the executor authorization and suspension justification.
     * @return A {@link Mono} emitting a {@link MutableHttpResponse} confirming the suspension details.
     * @throws io.micronaut.http.exceptions.HttpStatusException containing a 409 CONFLICT status if the record is already inactive/revoked.
     */
    @Patch("/{id}/deactivate")
    @Operation(
            summary = "Deactivate a hash",
            description = "Transitions an ACTIVE hash to an INACTIVE status. This operation is non-destructive, fully reversible, and audited."
    )
    @ApiResponse(responseCode = "200", description = "Hash successfully transitioned to INACTIVE state and audited.")
    @ApiResponse(responseCode = "400", description = "Invalid request payload or malformed UUID parameter.")
    @ApiResponse(responseCode = "404", description = "No hash record exists for the provided system identifier.")
    @ApiResponse(responseCode = "409", description = "State transition conflict: hash is already INACTIVE or permanently REVOKED.")
    public Mono<MutableHttpResponse<DeactivateHashResponse>> deactivate(
            @PathVariable @Parameter(name = "id", description = "The immutable UUID of the target hash registry", required = true, example = "9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d") String id,
            @Body @Valid @Parameter(description = "Deactivation request payload detailing executor and justification", required = true) DeactivateHashRequest request
    ) {
        return deactivateHashUseCase.execute(request.toCommand(id))
                .map(token -> DeactivateHashResponse.fromDomain(token, request.executor(), request.reason()))
                .map(HttpResponse::ok)
                .doOnSubscribe(s -> log.info("[ACTION: DEACTIVATE_HASH] [ID: {}] [EXECUTOR: {}] - Initiating status suspension. Reason: {}", id, request.executor(), request.reason()))
                .doOnSuccess(res -> log.info("[ACTION: DEACTIVATE_HASH] [ID: {}] - Entity status successfully transitioned to INACTIVE.", id))
                .doOnError(err -> log.error("[ACTION: DEACTIVATE_HASH] [ID: {}] - Deactivation sequence failed: {}", id, err.getMessage()));
    }

    /**
     * Restores an inactive hash to its operational state (ACTIVE).
     * <p>Enables the hash to resume operational activities. Validates that the record is in a non-terminal reversible state.</p>
     *
     * @param id      The internal system identifier (UUID) of the target hash.
     * @param request The {@link ReactivateHashRequest} containing the executor context.
     * @return A {@link Mono} emitting a {@link MutableHttpResponse} with the restored {@link HashResponse} record.
     * @throws io.micronaut.http.exceptions.HttpStatusException containing a 409 CONFLICT status if the record is already ACTIVE or permanently REVOKED.
     */
    @Patch("/{id}/reactivate")
    @Operation(
            summary = "Reactivate a hash",
            description = "Restores an INACTIVE hash registry back to its ACTIVE operational state. This action is audited."
    )
    @ApiResponse(responseCode = "200", description = "Hash successfully restored to ACTIVE state and audited.")
    @ApiResponse(responseCode = "400", description = "Invalid request payload or malformed UUID parameter.")
    @ApiResponse(responseCode = "404", description = "No hash record exists for the provided system identifier.")
    @ApiResponse(responseCode = "409", description = "State transition conflict: hash cannot be reactivated (e.g., currently ACTIVE or permanently REVOKED).")
    public Mono<MutableHttpResponse<HashResponse>> reactivate(
            @PathVariable @Parameter(name = "id", description = "The immutable UUID of the target hash registry", required = true, example = "9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d") String id,
            @Body @Valid @Parameter(description = "Reactivation request payload detailing executor authorization", required = true) ReactivateHashRequest request
    ) {
        return reactivateHashUseCase.execute(request.toCommand(id))
                .map(HashResponse::fromDomain)
                .map(HttpResponse::ok)
                .doOnSubscribe(s -> log.info("[ACTION: REACTIVATE_HASH] [ID: {}] [EXECUTOR: {}] - Initiating status restoration.", id, request.executor()))
                .doOnSuccess(res -> log.info("[ACTION: REACTIVATE_HASH] [ID: {}] - Entity status successfully restored to ACTIVE.", id))
                .doOnError(err -> log.error("[ACTION: REACTIVATE_HASH] [ID: {}] - Reactivation sequence failed: {}", id, err.getMessage()));
    }

    /**
     * Permanently and irreversibly revokes a hash registry, transitioning it to a terminal REVOKED state.
     * <p><b>Destructive Operation (Zero Trust):</b> This transitions the entity to an irreversible terminal state.
     * Any subsequent state mutations will be blocked forever. Requires maximum logging severity.</p>
     *
     * @param id      The internal system identifier (UUID) of the target hash.
     * @param request The {@link RevokeHashRequest} containing the revoker's credentials and justification.
     * @return A {@link Mono} emitting a {@link MutableHttpResponse} confirming the terminal revocation.
     * @throws io.micronaut.http.exceptions.HttpStatusException containing a 409 CONFLICT status if the record is already REVOKED.
     */
    @Delete("/{id}")
    @Operation(
            summary = "Revoke a hash",
            description = "Irreversibly transitions a hash registry to the terminal REVOKED state. Destructive operation that blocks future mutations."
    )
    @ApiResponse(responseCode = "200", description = "Hash permanently revoked and audited successfully.")
    @ApiResponse(responseCode = "400", description = "Invalid revocation payload or malformed UUID parameter.")
    @ApiResponse(responseCode = "404", description = "No hash record exists for the provided system identifier.")
    @ApiResponse(responseCode = "409", description = "State conflict: hash is already in a terminal REVOKED state.")
    public Mono<MutableHttpResponse<HashResponse>> revoke(
            @PathVariable @Parameter(name = "id", description = "The immutable UUID of the target hash registry", required = true, example = "9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d") String id,
            @Body @Valid @Parameter(description = "Revocation request payload containing justification and elevated authority", required = true) RevokeHashRequest request
    ) {
        return revokeHashUseCase.execute(request.toCommand(id))
                .map(HashResponse::fromDomain)
                .map(HttpResponse::ok)
                .doOnSubscribe(s -> log.warn("[ACTION: REVOKE_HASH] [ID: {}] [EXECUTOR: {}] - CRITICAL: Initiating permanent and irreversible entity revocation. Reason: {}", id, request.executor(), request.reason()))
                .doOnSuccess(res -> log.warn("[ACTION: REVOKE_HASH] [ID: {}] - CRITICAL: Entity permanently transitioned to terminal REVOKED state.", id))
                .doOnError(err -> log.error("[ACTION: REVOKE_HASH] [ID: {}] - CRITICAL: Revocation sequence aborted due to failure: {}", id, err.getMessage()));
    }

    /**
     * Retrieves the entire immutable forensic audit trail mapped to a specific cryptographic hash.
     * <p>This audit trail lists all state transitions, reasons, timestamps, and executors in chronological order.</p>
     *
     * @param id The targeted business entity identifier (Hash ID).
     * @return A {@link Mono} emitting a {@link MutableHttpResponse} containing the sequential audit history list.
     * @throws io.micronaut.http.exceptions.HttpStatusException containing a 404 NOT FOUND if the target entity does not exist.
     */
    @Get("/{id}/audit")
    @Operation(
            summary = "Get audit trail",
            description = "Retrieves the complete immutable forensic history of state mutations, deactivations, reactivations, or revocations for a specific hash."
    )
    @ApiResponse(responseCode = "200", description = "Audit trail successfully found and projected.")
    @ApiResponse(responseCode = "400", description = "Invalid or blank entity identifier format.")
    @ApiResponse(responseCode = "404", description = "No audit log history exists for the provided identifier.")
    public Mono<MutableHttpResponse<List<HashAuditResponse>>> getAuditTrail(
            @PathVariable @Parameter(name = "id", description = "The business entity identifier (Hash ID) to fetch forensic history for", required = true, example = "9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d") String id
    ) {
        return getAuditLogsUseCase.execute(id)
                .map(HashAuditResponse::fromDomain)
                .collectList()
                .map(HttpResponse::ok)
                .doOnSubscribe(s -> log.info("[ACTION: GET_AUDIT] [ID: {}] - Initiating extraction of immutable forensic state mutations.", id))
                .doOnSuccess(res -> log.info("[ACTION: GET_AUDIT] [ID: {}] - Forensic trail successfully extracted. Total historical events projected: {}", id, res.body() != null ? res.body().size() : 0))
                .doOnError(err -> log.error("[ACTION: GET_AUDIT] [ID: {}] - Forensic trail extraction failed: {}", id, err.getMessage()));
    }
}