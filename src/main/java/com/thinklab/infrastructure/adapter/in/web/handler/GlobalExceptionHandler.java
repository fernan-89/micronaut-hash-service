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

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Infrastructure Adapter: Global Exception Handler for centralized error management.
 * <p>This adapter intercepts all domain and infrastructure exceptions, translating them
 * into standardized RFC 7807 "Problem Details" responses. It ensures that the API
 * remains consistent, predictable, and fully observable for forensic analysis.</p>
 *
 * <p><b>Architectural Principles (Mission-Critical Pattern):</b></p>
 * <ul>
 * <li><b>Error Protocol:</b> Enforces standardized error payloads for all failure modes.</li>
 * <li><b>Observability:</b> Logs comprehensive diagnostic context for internal failures (500).</li>
 * <li><b>Boundary Security:</b> Masks technical implementation details from external consumers in production scenarios.</li>
 * <li><b>Separation of Concerns:</b> Categorizes failures into Business (4xx) and Technical (5xx) domains.</li>
 * </ul>
 */
@Slf4j
@Produces
@Singleton
@Requires(classes = {ExceptionHandler.class})
public class GlobalExceptionHandler implements ExceptionHandler<Throwable, HttpResponse<Map<String, Object>>> {

    @Override
    public HttpResponse<Map<String, Object>> handle(HttpRequest request, Throwable exception) {
        log.error("[ACTION: GLOBAL_EXCEPTION_HANDLER] - CRITICAL: Pipeline aborted. Path: [{}]. Error: {}",
                request.getPath(), exception.getMessage(), exception);

        if (exception instanceof BusinessException businessEx) {
            return handleBusinessException(businessEx);
        }

        if (exception instanceof ConstraintViolationException constraintEx) {
            return handleValidationException(constraintEx);
        }

        return handleGenericException(exception);
    }

    private HttpResponse<Map<String, Object>> handleBusinessException(BusinessException ex) {
        HttpStatus status = switch (ex.getErrorCode()) {
            case "HASH_NOT_FOUND" -> HttpStatus.NOT_FOUND;
            case "INVALID_STATE_TRANSITION" -> HttpStatus.CONFLICT;
            case "DUPLICATE_RESOURCE" -> HttpStatus.UNPROCESSABLE_ENTITY;
            default -> HttpStatus.BAD_REQUEST;
        };

        return HttpResponse.status(status)
                .body(createProblem(ex.getErrorCode(), ex.getMessage(), status.getCode()));
    }

    private HttpResponse<Map<String, Object>> handleValidationException(ConstraintViolationException ex) {
        return HttpResponse.badRequest()
                .body(createProblem("VALIDATION_ERROR", "Invalid input parameters provided.", 400));
    }

    private HttpResponse<Map<String, Object>> handleGenericException(Throwable ex) {
        Map<String, Object> body = createProblem("INTERNAL_SERVER_ERROR", "An unexpected technical failure occurred.", 500);

        body.put("debug_info", ex.getClass().getSimpleName() + ": " + ex.getMessage());

        return HttpResponse.serverError().body(body);
    }

    private Map<String, Object> createProblem(String code, String message, int status) {
        Map<String, Object> problem = new LinkedHashMap<>();
        problem.put("timestamp", Instant.now().toString());
        problem.put("status", status);
        problem.put("error_code", code);
        problem.put("message", message);
        return problem;
    }
}