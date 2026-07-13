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
 */
@Slf4j
@Controller("/hashes")
@RequiredArgsConstructor
@Tag(name = "Hash Registry API", description = "Endpoints for managing cryptographic hash lifecycles and serial keys.")
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
     *
     * @param request The {@link GenerateHashRequest} payload containing tenant metadata and hashing algorithm.
     * @return A {@link Mono} emitting a {@link MutableHttpResponse} with the generated {@link HashResponse} and 201 Created status.
     */
    @Post
    @Operation(summary = "Generate a new hash", description = "Calculates a cryptographic hash, persists it securely, and logs the creation audit event.")
    @ApiResponse(responseCode = "201", description = "Hash generated and audited successfully.")
    @ApiResponse(responseCode = "400", description = "Invalid request payload (e.g., missing required fields, invalid algorithm).")
    @ApiResponse(responseCode = "500", description = "Internal server error during hash generation.")
    public Mono<MutableHttpResponse<HashResponse>> generate(@Body @Valid GenerateHashRequest request) {
        return generateHashUseCase.execute(request.toCommand())
                .map(HashResponse::fromDomain)
                .map(HttpResponse::created)
                .doOnSubscribe(s -> log.info("[ACTION: GENERATE_HASH] [TENANT: {}] [ALGO: {}] - Initiating entity creation protocol.", request.tenantId(), request.algorithm()))
                .doOnSuccess(res -> log.info("[ACTION: GENERATE_HASH] [TENANT: {}] - Entity creation successfully completed. Status: 201 CREATED.", request.tenantId()))
                .doOnError(err -> log.error("[ACTION: GENERATE_HASH] [TENANT: {}] - Entity creation protocol failed: {}", request.tenantId(), err.getMessage()));
    }

    /**
     * Retrieves a single hash registry record by its internal system identifier.
     *
     * @param id The unique internal identifier (UUID) of the hash registry.
     * @return A {@link Mono} emitting a {@link MutableHttpResponse} with the {@link HashResponse} record.
     */
    @Get("/{id}")
    @Operation(summary = "Get hash by ID", description = "Retrieves the sanitized metadata and current state of a specific hash.")
    @ApiResponse(responseCode = "200", description = "Hash record found and returned.")
    @ApiResponse(responseCode = "400", description = "Malformed identifier format provided.")
    @ApiResponse(responseCode = "404", description = "Hash not found for the provided identifier.")
    public Mono<MutableHttpResponse<HashResponse>> getById(@PathVariable String id) {
        return getHashUseCase.execute(new GetHashQuery(id))
                .map(HashResponse::fromDomain)
                .map(HttpResponse::ok)
                .doOnSubscribe(s -> log.info("[ACTION: GET_HASH] [ID: {}] - Initiating secure retrieval of entity metadata and current state.", id))
                .doOnSuccess(res -> log.info("[ACTION: GET_HASH] [ID: {}] - Entity metadata successfully retrieved and projected.", id))
                .doOnError(err -> log.error("[ACTION: GET_HASH] [ID: {}] - Secure retrieval query failed: {}", id, err.getMessage()));
    }

    /**
     * Lists hashes for a specific tenant with optional status filtering and pagination.
     *
     * @param tenantId The unique identifier of the tenant, provided via the 'X-Tenant-Id' header.
     * @param status   Optional filter to restrict results by their current {@link HashStatus} (e.g., ACTIVE, INACTIVE).
     * @param page     The page index for pagination (zero-based). Default is 0.
     * @param size     The number of records per page. Default is 20.
     * @return A {@link Mono} emitting a {@link MutableHttpResponse} containing a {@link PagedHashResponse}.
     */
    @Get
    @Operation(summary = "List tenant hashes", description = "Returns a paginated stream of hashes belonging to a specific tenant.")
    @ApiResponse(responseCode = "200", description = "Paginated list of hashes retrieved successfully.")
    @ApiResponse(responseCode = "400", description = "Missing required 'X-Tenant-Id' header or invalid pagination parameters.")
    @ApiResponse(responseCode = "500", description = "Internal server error during data retrieval.")
    public Mono<MutableHttpResponse<PagedHashResponse>> list(
            @Header("X-Tenant-Id") @NotBlank String tenantId,
            @QueryValue @Nullable HashStatus status,
            @QueryValue(defaultValue = "0") int page,
            @QueryValue(defaultValue = "20") int size) {

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
     *
     * @param id      The internal system identifier of the hash.
     * @param request The {@link DeactivateHashRequest} containing the authorization context and reason.
     * @return A {@link Mono} emitting a {@link MutableHttpResponse} with confirmation of the state transition.
     */
    @Patch("/{id}/deactivate")
    @Operation(summary = "Deactivate a hash", description = "Transitions an ACTIVE hash to INACTIVE status. Operation is audited.")
    @ApiResponse(responseCode = "200", description = "Hash deactivated successfully.")
    @ApiResponse(responseCode = "400", description = "Invalid request payload or validation failure.")
    @ApiResponse(responseCode = "404", description = "Hash not found for the provided identifier.")
    @ApiResponse(responseCode = "409", description = "Hash is already in an INACTIVE or REVOKED state.")
    public Mono<MutableHttpResponse<DeactivateHashResponse>> deactivate(
            @PathVariable String id,
            @Body @Valid DeactivateHashRequest request) {

        return deactivateHashUseCase.execute(request.toCommand(id))
                .map(token -> DeactivateHashResponse.fromDomain(token, request.executor(), request.reason()))
                .map(HttpResponse::ok)
                .doOnSubscribe(s -> log.info("[ACTION: DEACTIVATE_HASH] [ID: {}] [EXECUTOR: {}] - Initiating status suspension. Reason: {}", id, request.executor(), request.reason()))
                .doOnSuccess(res -> log.info("[ACTION: DEACTIVATE_HASH] [ID: {}] - Entity status successfully transitioned to INACTIVE.", id))
                .doOnError(err -> log.error("[ACTION: DEACTIVATE_HASH] [ID: {}] - Deactivation sequence failed: {}", id, err.getMessage()));
    }

    /**
     * Restores an inactive hash to its operational state by transitioning it to ACTIVE.
     *
     * @param id      The internal system identifier of the hash.
     * @param request The {@link ReactivateHashRequest} containing the authorization context.
     * @return A {@link Mono} emitting a {@link MutableHttpResponse} with the updated hash record.
     */
    @Patch("/{id}/reactivate")
    @Operation(summary = "Reactivate a hash", description = "Restores an INACTIVE hash to ACTIVE status. Operation is audited.")
    @ApiResponse(responseCode = "200", description = "Hash reactivated successfully.")
    @ApiResponse(responseCode = "400", description = "Invalid request payload or validation failure.")
    @ApiResponse(responseCode = "404", description = "Hash not found for the provided identifier.")
    @ApiResponse(responseCode = "409", description = "Hash cannot be reactivated (e.g., currently ACTIVE or permanently REVOKED).")
    public Mono<MutableHttpResponse<HashResponse>> reactivate(
            @PathVariable String id,
            @Body @Valid ReactivateHashRequest request) {

        return reactivateHashUseCase.execute(request.toCommand(id))
                .map(HashResponse::fromDomain)
                .map(HttpResponse::ok)
                .doOnSubscribe(s -> log.info("[ACTION: REACTIVATE_HASH] [ID: {}] [EXECUTOR: {}] - Initiating status restoration.", id, request.executor()))
                .doOnSuccess(res -> log.info("[ACTION: REACTIVATE_HASH] [ID: {}] - Entity status successfully restored to ACTIVE.", id))
                .doOnError(err -> log.error("[ACTION: REACTIVATE_HASH] [ID: {}] - Reactivation sequence failed: {}", id, err.getMessage()));
    }

    /**
     * Permanently and irreversibly revokes a hash registry, transitioning it to a terminal REVOKED state.
     * <p><b>Note:</b> This is a destructive operation (Zero Trust) and must be performed with elevated privileges.</p>
     *
     * @param id      The internal system identifier of the hash.
     * @param request The {@link RevokeHashRequest} indicating the revoker's credentials and justification.
     * @return A {@link Mono} emitting a {@link MutableHttpResponse} confirming the terminal revocation.
     */
    @Delete("/{id}")
    @Operation(summary = "Revoke a hash", description = "Irreversibly transitions a hash to the REVOKED status (Zero Trust). Operation is heavily audited.")
    @ApiResponse(responseCode = "200", description = "Hash permanently revoked and audited successfully.")
    @ApiResponse(responseCode = "400", description = "Invalid request payload or validation failure.")
    @ApiResponse(responseCode = "404", description = "Hash not found for the provided identifier.")
    @ApiResponse(responseCode = "409", description = "Hash is already in a REVOKED terminal state.")
    public Mono<MutableHttpResponse<HashResponse>> revoke(
            @PathVariable String id,
            @Body @Valid RevokeHashRequest request) {

        return revokeHashUseCase.execute(request.toCommand(id))
                .map(HashResponse::fromDomain)
                .map(HttpResponse::ok)
                .doOnSubscribe(s -> log.warn("[ACTION: REVOKE_HASH] [ID: {}] [EXECUTOR: {}] - CRITICAL: Initiating permanent and irreversible entity revocation. Reason: {}", id, request.executor(), request.reason()))
                .doOnSuccess(res -> log.warn("[ACTION: REVOKE_HASH] [ID: {}] - CRITICAL: Entity permanently transitioned to terminal REVOKED state.", id))
                .doOnError(err -> log.error("[ACTION: REVOKE_HASH] [ID: {}] - CRITICAL: Revocation sequence aborted due to failure: {}", id, err.getMessage()));
    }

    /**
     * Retrieves the entire immutable forensic audit trail mapped to a specific cryptographic hash.
     *
     * @param id The targeted business entity identifier (Hash ID).
     * @return A {@link Mono} emitting a {@link MutableHttpResponse} containing the list of sequential audit logs.
     */
    @Get("/{id}/audit")
    @Operation(summary = "Get audit trail", description = "Retrieves the complete immutable forensic history of state mutations for a specific hash.")
    @ApiResponse(responseCode = "200", description = "Audit logs retrieved successfully.")
    @ApiResponse(responseCode = "400", description = "Invalid or blank entity identifier format.")
    @ApiResponse(responseCode = "404", description = "Hash not found for the provided identifier.")
    public Mono<MutableHttpResponse<List<HashAuditResponse>>> getAuditTrail(@PathVariable String id) {
        return getAuditLogsUseCase.execute(id)
                .map(HashAuditResponse::fromDomain)
                .collectList()
                .map(HttpResponse::ok)
                .doOnSubscribe(s -> log.info("[ACTION: GET_AUDIT] [ID: {}] - Initiating extraction of immutable forensic state mutations.", id))
                .doOnSuccess(res -> log.info("[ACTION: GET_AUDIT] [ID: {}] - Forensic trail successfully extracted. Total historical events projected: {}", id, res.body() != null ? res.body().size() : 0))
                .doOnError(err -> log.error("[ACTION: GET_AUDIT] [ID: {}] - Forensic trail extraction failed: {}", id, err.getMessage()));
    }
}