package com.thinklab.infrastructure.adapter.in.web.controller;

import com.thinklab.application.command.GetHashQuery;
import com.thinklab.application.command.ListHashesQuery;
import com.thinklab.application.port.in.*;
import com.thinklab.domain.valueobject.HashStatus;
import com.thinklab.infrastructure.adapter.in.web.dto.request.DeactivateHashRequest;
import com.thinklab.infrastructure.adapter.in.web.dto.request.GenerateHashRequest;
import com.thinklab.infrastructure.adapter.in.web.dto.request.ReactivateHashRequest;
import com.thinklab.infrastructure.adapter.in.web.dto.request.RevokeHashRequest;
import com.thinklab.infrastructure.adapter.in.web.dto.response.HashResponse;
import com.thinklab.infrastructure.adapter.in.web.dto.response.PagedHashResponse;
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

/**
 * REST Controller: The primary inbound adapter for the Hash Service.
 * This class acts as a thin mediation layer between the HTTP transport protocol
 * and the Core Application UseCases. It focuses on protocol translation,
 * input validation, and secure data projection.
 *
 * <p><b>Architectural Principles:</b></p>
 * <ul>
 *     <li><b>Non-blocking:</b> Leverages Project Reactor for asynchronous processing.</li>
 *     <li><b>Decoupled:</b> Depends only on Input Ports (Interfaces).</li>
 *     <li><b>Defensive:</b> Enforces strict DTO validation at the boundary.</li>
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

    /**
     * Generates a new cryptographic hash or serial key.
     */
    @Post
    @Operation(summary = "Generate a new hash", description = "Calculates a cryptographic hash and persists it.")
    @ApiResponse(responseCode = "201", description = "Hash generated and audited successfully")
    public Mono<MutableHttpResponse<HashResponse>> generate(@Body @Valid GenerateHashRequest request) { // <-- Alterado aqui
        log.info("REST Request: Generation intent for tenant [{}] using algorithm [{}]",
                request.tenantId(), request.algorithm());

        return generateHashUseCase.execute(request.toCommand())
                .map(HashResponse::fromDomain)
                .map(HttpResponse::created)
                .doOnSuccess(res -> log.info("REST Response: Hash generated successfully"));
    }

    /**
     * Retrieves a single hash registry by its internal system identifier.
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
     * Suspends a hash operational status (transitions to INACTIVE).
     */
    @Patch("/{id}/deactivate")
    @Operation(summary = "Deactivate a hash", description = "Transitions an active hash to INACTIVE status.")
    @ApiResponse(responseCode = "200", description = "Hash deactivated successfully")
    public Mono<HttpResponse<HashResponse>> deactivate(
            @PathVariable String id,
            @Body @Valid DeactivateHashRequest request) {

        log.warn("REST Request: Deactivation intent for ID [{}] authorized by [{}]", id, request.executor());

        return deactivateHashUseCase.execute(request.toCommand(id))
                .map(HashResponse::fromDomain)
                .map(HttpResponse::ok);
    }

    /**
     * Restores an inactive hash to its operational state (transitions to ACTIVE).
     */
    @Patch("/{id}/reactivate")
    @Operation(summary = "Reactivate a hash", description = "Restores an INACTIVE hash to ACTIVE status.")
    public Mono<HttpResponse<HashResponse>> reactivate(
            @PathVariable String id,
            @Body @Valid ReactivateHashRequest request) {

        log.info("REST Request: Reactivation intent for ID [{}]", id);

        return reactivateHashUseCase.execute(request.toCommand(id))
                .map(HashResponse::fromDomain)
                .map(HttpResponse::ok);
    }

    /**
     * Permanently and irreversibly revokes a hash registry (terminal state).
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
}