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
 * Global Exception Handler: Centralized reactive error transformation.
 * This adapter intercepts domain and infrastructure exceptions, translating them
 * into a standardized RFC 7807 "Problem Details" response.
 *
 * <p><b>Design Principles:</b></p>
 * <ul>
 * <li><b>Observability:</b> Logs full stack traces for technical failures (500).</li>
 * <li><b>Non-blocking:</b> Fully integrated into Micronaut's Netty pipeline.</li>
 * <li><b>Categorization:</b> Separates Business Errors (4xx) from Technical Failures (500).</li>
 * </ul>
 */
@Slf4j
@Produces
@Singleton
@Requires(classes = {ExceptionHandler.class})
public class GlobalExceptionHandler implements ExceptionHandler<Throwable, HttpResponse<Map<String, Object>>> {

    @Override
    public HttpResponse<Map<String, Object>> handle(HttpRequest request, Throwable exception) {
        // Log detalhado com stacktrace completa para depuração técnica (Nível NASA)
        log.error("Global Exception Captured: Path: [{}] | Error: {}", request.getPath(), exception.getMessage(), exception);

        // 1. Handle Domain Business Exceptions (e.g., 404, 409, 422)
        if (exception instanceof BusinessException businessEx) {
            return handleBusinessException(businessEx);
        }

        // 2. Handle Validation Exceptions (Input Boundary Failures)
        if (exception instanceof ConstraintViolationException constraintEx) {
            return handleValidationException(constraintEx);
        }

        // 3. Fallback for unexpected technical failures (HTTP 500)
        return handleGenericException(exception);
    }

    private HttpResponse<Map<String, Object>> handleBusinessException(BusinessException ex) {
        HttpStatus status = switch (ex.getErrorCode()) {
            case "HASH_NOT_FOUND" -> HttpStatus.NOT_FOUND;
            case "INVALID_STATE_TRANSITION" -> HttpStatus.CONFLICT;
            case "DUPLICATE_RESOURCE" -> HttpStatus.UNPROCESSABLE_ENTITY;
            default -> HttpStatus.BAD_REQUEST;
        };

        return HttpResponse.status(status).body(createProblem(ex.getErrorCode(), ex.getMessage(), status.getCode()));
    }

    private HttpResponse<Map<String, Object>> handleValidationException(ConstraintViolationException ex) {
        return HttpResponse.badRequest().body(createProblem("VALIDATION_ERROR", "Invalid input parameters provided.", 400));
    }

    private HttpResponse<Map<String, Object>> handleGenericException(Throwable ex) {
        Map<String, Object> body = createProblem("INTERNAL_SERVER_ERROR", "An unexpected technical failure occurred.", 500);
        // Expondo detalhes da exceção apenas em falhas 500 para facilitar o debug imediato
        body.put("details", ex.getClass().getSimpleName() + ": " + ex.getMessage());
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