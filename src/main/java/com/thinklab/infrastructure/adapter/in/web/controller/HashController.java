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
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

/**
 * REST Controller: The primary inbound adapter for the Hash Service.
 *
 * <p><b>Architectural Role:</b>
 * Acts as a thin, highly cohesive mediation layer between the HTTP transport protocol and the
 * Core Application UseCases (Ports). It focuses exclusively on protocol translation, strict input
 * validation (JSR 380), and secure data projection, ensuring absolute isolation of the domain layer.
 *
 * <p><b>Contractual Obligations:</b>
 * <ul>
 * <li><b>Reactive Purity:</b> 100% non-blocking. Leverages Project Reactor ({@link Mono}) to ensure
 *     the Netty EventLoop is never stalled.</li>
 * <li><b>Identity Sovereignty:</b> Enforces {@link UUID} strictly at the API boundary, automatically
 *     rejecting malformed requests with 400 Bad Request before hitting business logic.</li>
 * <li><b>Constructor Injection:</b> Explicitly avoids Lombok generated constructors (ADR-001) to
 *     guarantee deterministic dependency injection and proxying by Micronaut AOP.</li>
 * </ul>
 *
 * <p><b>Telemetry & Observability:</b>
 * Adheres strictly to the structured logging format: {@code [ACTION: NAME] [ID: UUID]}.
 * Emits signals via Reactor lifecycle hooks ({@code doOnSubscribe}, {@code doOnSuccess}, {@code doOnError})
 * without disrupting the asynchronous data stream.
 *
 * @author Thinklab Systems Engineering Team
 * @version 3.4.1-NASA-SRE-PROD
 * @since 1.0
 */
@Slf4j
@Controller("/hashes")
@Tag(name = "Hash Registry Lifecycle", description = "High-Assurance API for generating, querying, and auditing cryptographic hashes under Zero-Trust constraints.")
public class HashController {

    private final GenerateHashUseCase generateHashUseCase;
    private final GetHashUseCase getHashUseCase;
    private final ListHashesUseCase listHashesUseCase;
    private final DeactivateHashUseCase deactivateHashUseCase;
    private final ReactivateHashUseCase reactivateHashUseCase;
    private final RevokeHashUseCase revokeHashUseCase;
    private final GetAuditLogsUseCase getAuditLogsUseCase;

    /**
     * Explicit constructor for strict dependency injection (ADR-001).
     *
     * @param generateHashUseCase Inbound port for hash creation operations.
     * @param getHashUseCase      Inbound port for single record retrieval.
     * @param listHashesUseCase   Inbound port for paginated multitenant retrieval.
     * @param deactivateHashUseCase Inbound port for temporary suspension.
     * @param reactivateHashUseCase Inbound port for restoring suspended hashes.
     * @param revokeHashUseCase   Inbound port for irreversible terminal revocation.
     * @param getAuditLogsUseCase Inbound port for retrieving forensic event logs.
     */
    @Inject
    public HashController(
            GenerateHashUseCase generateHashUseCase,
            GetHashUseCase getHashUseCase,
            ListHashesUseCase listHashesUseCase,
            DeactivateHashUseCase deactivateHashUseCase,
            ReactivateHashUseCase reactivateHashUseCase,
            RevokeHashUseCase revokeHashUseCase,
            GetAuditLogsUseCase getAuditLogsUseCase
    ) {
        this.generateHashUseCase = generateHashUseCase;
        this.getHashUseCase = getHashUseCase;
        this.listHashesUseCase = listHashesUseCase;
        this.deactivateHashUseCase = deactivateHashUseCase;
        this.reactivateHashUseCase = reactivateHashUseCase;
        this.revokeHashUseCase = revokeHashUseCase;
        this.getAuditLogsUseCase = getAuditLogsUseCase;
    }

    /**
     * Generates a new cryptographic hash or serial key based on tenant specifications.
     *
     * <p>This operation initializes the lifecycle of a cryptographic record within the system.
     * The payload is rigorously validated at the boundary.
     *
     * @param request The {@link GenerateHashRequest} payload containing tenant metadata and hashing algorithm.
     * @return A {@link Mono} emitting a {@link MutableHttpResponse} containing the generated {@link HashResponse} record with a 201 CREATED status.
     */
    @Post
    @Operation(
            summary = "Generate a new cryptographic hash",
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
     * Retrieves a single hash registry record by its internal BSON-compliant UUID.
     *
     * <p>Provides a sanitized projection of the target hash record, filtering out sensitive internal domain structures.
     *
     * @param id The universally unique identifier (UUID) of the hash registry.
     * @return A {@link Mono} emitting a {@link MutableHttpResponse} with the {@link HashResponse} metadata block.
     */
    @Get("/{id}")
    @Operation(
            summary = "Fetch a hash state by UUID",
            description = "Retrieves the sanitized metadata, current operational state, and algorithms details of a specific hash registry."
    )
    @ApiResponse(responseCode = "200", description = "Hash record found and returned successfully.")
    @ApiResponse(responseCode = "400", description = "Malformed identifier format provided (must comply with UUID canonical standard).")
    @ApiResponse(responseCode = "404", description = "No hash record exists for the provided system identifier (RFC 7807 problem details returned).")
    public Mono<MutableHttpResponse<HashResponse>> getById(
            @PathVariable @Parameter(name = "id", description = "The immutable UUID of the cryptographic hash registry", required = true, example = "9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d") UUID id
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
     *
     * <p>Performs a secure, paginated multidimensional search strictly bounded to the requested tenant context.
     *
     * @param tenantId The unique identifier of the tenant context (X-Tenant-Id header).
     * @param status   Optional query parameter to restrict the result list by operational state.
     * @param page     The zero-based page index. Default is 0.
     * @param size     The maximum volume of records allowed in a single page stream. Default is 20.
     * @return A {@link Mono} emitting a {@link MutableHttpResponse} containing a {@link PagedHashResponse}.
     */
    @Get
    @Operation(
            summary = "Paginate tenant hashes",
            description = "Returns a paginated stream of hashes belonging strictly to the tenant context declared in the HTTP header."
    )
    @ApiResponse(responseCode = "200", description = "Paginated list of hashes retrieved and projected successfully.")
    @ApiResponse(responseCode = "400", description = "Missing or blank 'X-Tenant-Id' header, or invalid pagination range parameters.")
    @ApiResponse(responseCode = "500", description = "Internal server error occurred during multitenant lookup execution.")
    public Mono<MutableHttpResponse<PagedHashResponse>> list(
            @Header("X-Tenant-Id") @NotBlank @Parameter(in = ParameterIn.HEADER, name = "X-Tenant-Id", description = "Unique identifier of the tenant context to isolate multitenant queries", required = true, example = "tenant-prod-alpha-1") String tenantId,
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
     *
     * <p>Deactivation is a reversible state mutation. The action strictly enforces the internal domain state machine.
     *
     * @param id      The universally unique identifier (UUID) of the target hash.
     * @param request The {@link DeactivateHashRequest} detailing executor authorization and justification.
     * @return A {@link Mono} emitting a {@link MutableHttpResponse} confirming the suspension details.
     */
    @Patch("/{id}/deactivate")
    @Operation(
            summary = "Suspend hash operation (Deactivate)",
            description = "Transitions an ACTIVE hash to an INACTIVE status. This operation is non-destructive, fully reversible, and recorded in the audit trail."
    )
    @ApiResponse(responseCode = "200", description = "Hash successfully transitioned to INACTIVE state and audited.")
    @ApiResponse(responseCode = "400", description = "Invalid request payload or malformed UUID parameter.")
    @ApiResponse(responseCode = "404", description = "No hash record exists for the provided system identifier.")
    @ApiResponse(responseCode = "409", description = "State transition conflict: hash is already INACTIVE or permanently REVOKED.")
    public Mono<MutableHttpResponse<DeactivateHashResponse>> deactivate(
            @PathVariable @Parameter(name = "id", description = "The immutable UUID of the target hash registry", required = true, example = "9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d") UUID id,
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
     * Restores an inactive hash to its operational ACTIVE state.
     *
     * <p>Enables the hash to resume downstream validations. Fails instantly if the record is REVOKED.
     *
     * @param id      The universally unique identifier (UUID) of the target hash.
     * @param request The {@link ReactivateHashRequest} containing the executor context.
     * @return A {@link Mono} emitting a {@link MutableHttpResponse} with the restored {@link HashResponse}.
     */
    @Patch("/{id}/reactivate")
    @Operation(
            summary = "Restore hash operation (Reactivate)",
            description = "Restores an INACTIVE hash registry back to its ACTIVE operational state. Fails if the hash is already active or terminal."
    )
    @ApiResponse(responseCode = "200", description = "Hash successfully restored to ACTIVE state and audited.")
    @ApiResponse(responseCode = "400", description = "Invalid request payload or malformed UUID parameter.")
    @ApiResponse(responseCode = "404", description = "No hash record exists for the provided system identifier.")
    @ApiResponse(responseCode = "409", description = "State transition conflict: hash cannot be reactivated.")
    public Mono<MutableHttpResponse<HashResponse>> reactivate(
            @PathVariable @Parameter(name = "id", description = "The immutable UUID of the target hash registry", required = true, example = "9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d") UUID id,
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
     * Permanently and irreversibly revokes a hash registry.
     *
     * <p><b>Destructive Operation (Zero Trust):</b> Transitions the entity to a terminal state.
     * Triggers WARN-level telemetry for immediate Security Operation Center (SOC) visibility.
     *
     * @param id      The universally unique identifier (UUID) of the target hash.
     * @param request The {@link RevokeHashRequest} containing the revoker's credentials and justification.
     * @return A {@link Mono} emitting a {@link MutableHttpResponse} confirming the terminal revocation.
     */
    @Delete("/{id}")
    @Operation(
            summary = "Permanently revoke a hash",
            description = "Irreversibly transitions a hash registry to the terminal REVOKED state. Destructive operation that blocks future mutations."
    )
    @ApiResponse(responseCode = "200", description = "Hash permanently revoked and audited successfully.")
    @ApiResponse(responseCode = "400", description = "Invalid revocation payload or malformed UUID parameter.")
    @ApiResponse(responseCode = "404", description = "No hash record exists for the provided system identifier.")
    @ApiResponse(responseCode = "409", description = "State conflict: hash is already in a terminal REVOKED state.")
    public Mono<MutableHttpResponse<HashResponse>> revoke(
            @PathVariable @Parameter(name = "id", description = "The immutable UUID of the target hash registry", required = true, example = "9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d") UUID id,
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
     *
     * <p>Projects all state transitions, justifications, and executors in chronological order.
     *
     * @param id The deterministic business entity UUID (Hash ID).
     * @return A {@link Mono} emitting a {@link MutableHttpResponse} containing the sequential audit history.
     */
    @Get("/{id}/audit")
    @Operation(
            summary = "Fetch forensic audit trail",
            description = "Retrieves the complete immutable forensic history of state mutations, deactivations, reactivations, or revocations for a specific hash."
    )
    @ApiResponse(responseCode = "200", description = "Audit trail successfully found and projected.")
    @ApiResponse(responseCode = "400", description = "Invalid or malformed UUID identifier format.")
    @ApiResponse(responseCode = "404", description = "No audit log history exists for the provided identifier.")
    public Mono<MutableHttpResponse<List<HashAuditResponse>>> getAuditTrail(
            @PathVariable @Parameter(name = "id", description = "The deterministic entity UUID (Hash ID) to fetch forensic history for", required = true, example = "9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d") UUID id
    ) {
        return getAuditLogsUseCase.execute(id)
                .map(HashAuditResponse::fromDomain)
                .collectList()
                .map(HttpResponse::ok)
                .doOnSubscribe(s -> log.info("[ACTION: GET_AUDIT] [ID: {}] - Initiating extraction of immutable forensic state mutations.", id))
                .doOnSuccess(res -> log.info("[ACTION: GET_AUDIT] [ID: {}] - Forensic trail successfully extracted. Total historical events projected: {}", id, res.body() != null ? res.body().size() : 0))
                .doOnError(err -> log.error("[ACTION: GET_AUDIT] [ID: {}] - Forensic trail extraction failed: {}", id, err.getMessage()));
    }

    /**
     * Retrieves a consolidated 360-degree view (State + Chronological Audit Trail).
     * Implementation of ADR 003: Parallel projection for high-assurance discovery.
     *
     * @param id The target hash UUID.
     * @return A {@link Mono} emitting the consolidated {@link HashFullResponse} snapshot.
     */
    @Get("/{id}/details")
    @Operation(
            summary = "Fetch 360-degree aggregated hash view",
            description = "Implementation of ADR 003. Consolidates the current operational state of a hash registry with its full chronological forensic audit trail in a single parallel projection."
    )
    @ApiResponse(responseCode = "200", description = "Consolidated 360-degree projection successfully materialized.")
    @ApiResponse(responseCode = "400", description = "Invalid or malformed UUID identifier format.")
    @ApiResponse(responseCode = "404", description = "No hash record exists for the provided system identifier.")
    public Mono<MutableHttpResponse<HashFullResponse>> getFullView(
            @PathVariable @Parameter(name = "id", description = "The deterministic entity UUID (Hash ID)", required = true, example = "9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d") UUID id
    ) {
        return Mono.zip(
                        getHashUseCase.execute(new GetHashQuery(id)),
                        getAuditLogsUseCase.execute(id).collectList()
                )
                .map(tuple -> new HashFullResponse(
                        HashResponse.fromDomain(tuple.getT1()),
                        tuple.getT2().stream().map(HashAuditResponse::fromDomain).toList()
                ))
                .map(HttpResponse::ok)
                .doOnSubscribe(s -> log.info("[ACTION: GET_FULL_VIEW] [ID: {}] - Initiating 360-degree projection.", id))
                .doOnSuccess(res -> log.info("[ACTION: GET_FULL_VIEW] [ID: {}] - 360-degree projection successfully materialized.", id))
                .doOnError(err -> log.error("[ACTION: GET_FULL_VIEW] [ID: {}] - 360-degree projection failed: {}", id, err.getMessage()));
    }
}