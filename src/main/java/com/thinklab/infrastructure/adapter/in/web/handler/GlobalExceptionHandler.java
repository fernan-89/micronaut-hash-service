package com.thinklab.infrastructure.adapter.in.web.handler;

import com.thinklab.domain.exception.BusinessException;
import io.micronaut.context.annotation.Requires;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.annotation.Produces;
import io.micronaut.http.server.exceptions.ExceptionHandler;
import jakarta.inject.Singleton;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Infrastructure Adapter: Global Exception Handler for centralized error management.
 *
 * <p><b>Architectural Role:</b>
 * This adapter intercepts all domain, validation, and infrastructure exceptions thrown during the
 * lifecycle of an HTTP request. It acts as the final translation barrier, converting raw exceptions
 * into standardized <b>RFC 7807 "Problem Details"</b> JSON responses.
 *
 * <p><b>Contractual Obligations:</b>
 * <ul>
 * <li><b>RFC 7807 Standardization:</b> Enforces uniform error payloads containing type, title, status,
 *     detail, and request instance metadata across all microservice endpoints.</li>
 * <li><b>Separation of Concerns:</b> Cleanly bifurcates predictable business violations (HTTP 404, 409, 422)
 *     from catastrophic technical infrastructure failures (HTTP 500).</li>
 * <li><b>Boundary Security:</b> Masks sensitive internal stack traces and implementation details from external
 *     consumers in production environments while retaining rich diagnostics in server telemetry logs.</li>
 * </ul>
 *
 * @author ThinkLab
 * @since 1.0
 */
@Slf4j
@Produces
@Singleton
@Requires(classes = {ExceptionHandler.class})
public class GlobalExceptionHandler implements ExceptionHandler<Throwable, HttpResponse<Map<String, Object>>> {

    private static final String PROBLEM_TYPE_BASE_URI = "https://api.thinklab.com/errors/";

    /**
     * Intercepts any unhandled exception propagating to the HTTP transport layer and maps it
     * to a standardized RFC 7807 Problem Details response.
     *
     * @param request   The incoming HTTP request context.
     * @param exception The caught throwable exception from the reactive pipeline or controller boundary.
     * @return An HTTP response carrying the serialized RFC 7807 problem structure.
     */
    @Override
    public HttpResponse<Map<String, Object>> handle(HttpRequest request, Throwable exception) {
        Objects.requireNonNull(request, "HTTP request context cannot be null.");
        Objects.requireNonNull(exception, "Caught exception cannot be null.");

        String path = request.getPath();

        if (exception instanceof BusinessException businessEx) {
            log.info("[ACTION: GLOBAL_EXCEPTION_HANDLER] [PATH: {}] [CODE: {}] - Business rule violation intercepted: {}",
                    path, businessEx.getErrorCode(), businessEx.getMessage());
            return handleBusinessException(businessEx, path);
        }

        if (exception instanceof ConstraintViolationException constraintEx) {
            log.warn("[ACTION: GLOBAL_EXCEPTION_HANDLER] [PATH: {}] - JSR-380 input validation failure intercepted: {}",
                    path, constraintEx.getMessage());
            return handleValidationException(constraintEx, path);
        }

        log.error("[ACTION: GLOBAL_EXCEPTION_HANDLER] [PATH: {}] - CRITICAL: Unhandled technical failure encountered in pipeline: {}",
                path, exception.getMessage(), exception);
        return handleGenericException(exception, path);
    }

    /**
     * Maps known domain business exceptions to their corresponding HTTP status codes (404, 409, 422, etc.)
     * and constructs an RFC 7807 compliant problem body.
     */
    private HttpResponse<Map<String, Object>> handleBusinessException(BusinessException ex, String path) {
        HttpStatus status = switch (ex.getErrorCode()) {
            case "ERR-HASH-00404" -> HttpStatus.NOT_FOUND;
            case "ERR-HASH-00409" -> HttpStatus.CONFLICT;
            default -> HttpStatus.UNPROCESSABLE_ENTITY;
        };

        Map<String, Object> problem = createProblemDetails(
                URI.create(PROBLEM_TYPE_BASE_URI + ex.getErrorCode().toLowerCase().replace('_', '-')),
                ex.getErrorCode(),
                status.getCode(),
                status.getReason(),
                ex.getMessage(),
                path
        );

        return HttpResponse.status(status).body(problem);
    }

    /**
     * Maps edge validation failures (JSR-380) to HTTP 400 Bad Request.
     */
    private HttpResponse<Map<String, Object>> handleValidationException(ConstraintViolationException ex, String path) {
        Map<String, Object> problem = createProblemDetails(
                URI.create(PROBLEM_TYPE_BASE_URI + "err-validation-00400"),
                "ERR-VALIDATION-00400",
                HttpStatus.BAD_REQUEST.getCode(),
                HttpStatus.BAD_REQUEST.getReason(),
                "The request payload failed structural validation constraints: " + ex.getMessage(),
                path
        );

        return HttpResponse.badRequest().body(problem);
    }

    /**
     * Maps unforeseen technical exceptions to HTTP 500 Internal Server Error.
     */
    private HttpResponse<Map<String, Object>> handleGenericException(Throwable ex, String path) {
        Map<String, Object> problem = createProblemDetails(
                URI.create(PROBLEM_TYPE_BASE_URI + "err-internal-00500"),
                "ERR-INTERNAL-00500",
                HttpStatus.INTERNAL_SERVER_ERROR.getCode(),
                HttpStatus.INTERNAL_SERVER_ERROR.getReason(),
                "An unexpected technical failure occurred within the processing pipeline.",
                path
        );

        // Append safe debugging metadata for observability
        problem.put("debug_info", ex.getClass().getSimpleName() + ": " + ex.getMessage());

        return HttpResponse.serverError().body(problem);
    }

    /**
     * Factory helper to construct a strict RFC 7807 Problem Details map structure.
     */
    private Map<String, Object> createProblemDetails(
            URI type,
            String code,
            int status,
            String title,
            String detail,
            String instance
    ) {
        Map<String, Object> problem = new LinkedHashMap<>();
        problem.put("type", type.toString());
        problem.put("title", title);
        problem.put("status", status);
        problem.put("error_code", code);
        problem.put("detail", detail);
        problem.put("instance", instance);
        problem.put("timestamp", Instant.now().toString());
        return problem;
    }
}