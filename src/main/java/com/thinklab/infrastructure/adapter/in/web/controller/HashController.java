package com.thinklab.infrastructure.adapter.in.web.controller;

import com.thinklab.application.command.GetHashQuery;
import com.thinklab.application.command.ListHashesQuery;
import com.thinklab.application.port.in.*;
import com.thinklab.domain.model.HashAudit;
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
 * <li><b>Auditability:</b> Every state-changing request logs intent and outcome.</li>
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
    private final GetAuditLogsUseCase getAuditLogsUseCase; // 🚀 Injeção do UseCase de trilha forense

    /**
     * Generates a new cryptographic hash or serial key based on tenant specifications.
     *
     * @param request The {@link GenerateHashRequest} payload containing tenant metadata and hashing algorithm.
     * @return A {@link Mono} emitting a {@link MutableHttpResponse} with the generated {@link HashResponse} and 201 Created status.
     * @throws io.micronaut.http.HttpStatusException if input validation fails.
     */
    @Post
    @Operation(summary = "Generate a new hash", description = "Calculates a cryptographic hash and persists it.")
    @ApiResponse(responseCode = "201", description = "Hash generated and audited successfully")
    public Mono<MutableHttpResponse<HashResponse>> generate(@Body @Valid GenerateHashRequest request) {
        log.info("REST Request: Generation intent for tenant [{}] using algorithm [{}]",
                request.tenantId(), request.algorithm());

        return generateHashUseCase.execute(request.toCommand())
                .map(HashResponse::fromDomain)
                .map(HttpResponse::created)
                .doOnSuccess(res -> log.info("REST Response: Hash generated successfully"));
    }

    /**
     * Retrieves a single hash registry record by its internal system identifier (UUID).
     *
     * @param id The unique internal identifier of the hash registry.
     * @return A {@link Mono} emitting a {@link HttpResponse} with the {@link HashResponse} record.
     * @apiNote Returns 404 if no record exists for the provided identifier.
     */
    @Get("/{id}")
    @Operation(summary = "Get hash by ID", description = "Retrieves the sanitized metadata of a specific hash.")
    @ApiResponse(responseCode = "200", description = "Hash record found")
    @ApiResponse(responseCode = "404", description = "Hash not found")
    public Mono<HttpResponse<HashResponse>> getById(@PathVariable String id) {
        log.debug("REST Request: Retrieval for ID [{}]", id);

        return getHashUseCase.execute(new GetHashQuery(id))
                .map(HashResponse::fromDomain)
                .map(HttpResponse::ok);
    }

    /**
     * Lists hashes for a specific tenant with optional status filtering and pagination.
     *
     * @param tenantId The unique identifier of the tenant (provided via header).
     * @param status   Optional filter to restrict results by {@link HashStatus}.
     * @param page     Page index for pagination (default: 0).
     * @param size     Page size (default: 20).
     * @return A {@link Mono} emitting a {@link HttpResponse} containing a {@link PagedHashResponse}.
     */
    @Get
    @Operation(summary = "List tenant hashes", description = "Returns a paginated stream of hashes belonging to a tenant.")
    public Mono<HttpResponse<PagedHashResponse>> list(
            @Header("X-Tenant-Id") @NotBlank String tenantId,
            @QueryValue @Nullable HashStatus status,
            @QueryValue(defaultValue = "0") int page,
            @QueryValue(defaultValue = "20") int size) {

        log.debug("REST Request: Listing hashes for tenant [{}] - Page: [{}]", tenantId, page);

        var query = new ListHashesQuery(tenantId, status, page, size);

        return listHashesUseCase.execute(query)
                .map(HashResponse::fromDomain)
                .collectList()
                .map(content -> PagedHashResponse.of(content, 0, page, size))
                .map(HttpResponse::ok);
    }

    /**
     * Suspends a hash operational status by transitioning it to INACTIVE.
     *
     * @param id      The system identifier of the hash.
     * @param request Authorization and reason context for the deactivation action.
     * @return A {@link Mono} emitting a {@link HttpResponse} with confirmation of state transition.
     */
    @Patch("/{id}/deactivate")
    @Operation(summary = "Deactivate a hash", description = "Transitions an active hash to INACTIVE status.")
    @ApiResponse(responseCode = "200", description = "Hash deactivated successfully")
    public Mono<HttpResponse<DeactivateHashResponse>> deactivate(
            @PathVariable String id,
            @Body @Valid DeactivateHashRequest request) {

        log.warn("REST Request: Deactivation intent for ID [{}] authorized by [{}]", id, request.executor());

        return deactivateHashUseCase.execute(request.toCommand(id))
                .map(token -> DeactivateHashResponse.fromDomain(token, request.executor(), request.reason()))
                .map(HttpResponse::ok);
    }

    /**
     * Restores an inactive hash to its operational state (transitions to ACTIVE).
     *
     * @param id      The system identifier of the hash.
     * @param request Authorization context for the reactivation action.
     * @return A {@link Mono} emitting a {@link HttpResponse} with the updated hash record.
     */
    @Patch("/{id}/reactivate")
    @Operation(summary = "Reactivate a hash", description = "Restores an INACTIVE hash to ACTIVE status.")
    @ApiResponse(responseCode = "200", description = "Hash reactivated successfully")
    public Mono<HttpResponse<HashResponse>> reactivate(
            @PathVariable String id,
            @Body @Valid ReactivateHashRequest request) {

        log.info("REST Request: Reactivation intent for ID [{}]", id);

        return reactivateHashUseCase.execute(request.toCommand(id))
                .map(HashResponse::fromDomain)
                .map(HttpResponse::ok);
    }

    /**
     * Permanently and irreversibly revokes a hash registry, transitioning it to a terminal REVOKED state.
     * <p><b>Note:</b> This is a destructive operation and must be performed with elevated privileges.</p>
     *
     * @param id      The system identifier of the hash.
     * @param request Authorization context indicating the revoker's credentials.
     * @return A {@link Mono} emitting a {@link HttpResponse} confirming the terminal revocation.
     */
    @Delete("/{id}")
    @Operation(summary = "Revoke a hash", description = "Irreversibly transitions a hash to REVOKED status (Zero Trust).")
    @ApiResponse(responseCode = "200", description = "Hash permanently revoked and audited")
    public Mono<HttpResponse<HashResponse>> revoke(
            @PathVariable String id,
            @Body @Valid RevokeHashRequest request) {

        log.error("REST Request: CRITICAL - Revocation intent for ID [{}] authorized by [{}]", id, request.executor());

        return revokeHashUseCase.execute(request.toCommand(id))
                .map(HashResponse::fromDomain)
                .map(HttpResponse::ok);
    }

    /**
     * Retrieves the entire immutable forensic audit trail mapped to a specific cryptographic hash.
     *
     * @param id The targeted business entity identifier (Hash ID).
     * @return A {@link Mono} emitting a {@link HttpResponse} containing the list of audit logs.
     */
    @Get("/{id}/audit")
    @Operation(summary = "Get audit trail", description = "Retrieves the complete immutable forensic history of state mutations for a specific hash.")
    @ApiResponse(responseCode = "200", description = "Audit logs retrieved successfully")
    @ApiResponse(responseCode = "400", description = "Invalid or blank entity identifier")
    public Mono<HttpResponse<List<HashAuditResponse>>> getAuditTrail(@PathVariable String id) {
        log.debug("REST Request: Fetching forensic audit logs for Entity ID [{}]", id);

        return getAuditLogsUseCase.execute(id)
                .map(HashAuditResponse::fromDomain) // 🚀 Transforma cada HashAudit (Domínio) em HashAuditResponse (DTO)
                .collectList()                      // Agrupa o fluxo reativo em uma lista [HashAuditResponse]
                .map(HttpResponse::ok);             // Retorna HTTP 200 com a lista serializável
    }
}