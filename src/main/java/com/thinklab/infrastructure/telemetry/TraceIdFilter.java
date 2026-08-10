package com.thinklab.infrastructure.telemetry;

import io.micronaut.core.annotation.NonNull;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.MutableHttpResponse;
import io.micronaut.http.annotation.Filter;
import io.micronaut.http.filter.HttpServerFilter;
import io.micronaut.http.filter.ServerFilterChain;
import io.micronaut.http.filter.ServerFilterPhase;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Publisher;
import org.slf4j.MDC;
import reactor.core.publisher.Mono;
import reactor.util.context.Context;

import java.util.UUID;

/**
 * Infrastructure Component: Reactive MDC Traceability Filter.
 *
 * <p><b>Architectural Role:</b>
 * Intercepts all inbound HTTP traffic globally ("/**") at the Netty pipeline boundary.
 * Extracts the correlation ID from the request header or generates a fresh UUID, injecting it
 * into both SLF4J's MDC and Project Reactor's execution context for end-to-end trace propagation.
 *
 * <p><b>Contractual Obligations:</b>
 * <ul>
 * <li><b>Constructor Injection (ADR-001):</b> Utilizes standard bean lifecycle management.</li>
 * <li><b>Reactive Context Propagation:</b> Bridges Netty HTTP thread state with Project Reactor workers
 *     to ensure distributed tracing continuity across asynchronous execution boundaries.</li>
 * <li><b>MDC Cleanup:</b> Guarantees thread-context isolation by purging MDC attributes upon request finalization.</li>
 * </ul>
 *
 * @author ThinkLab
 * @version 2.1.0
 * @since 1.0
 */
@Singleton
@Filter("/**")
@Slf4j
public class TraceIdFilter implements HttpServerFilter {

    private static final String TRACE_ID_HEADER = "X-Trace-Id";
    private static final String MDC_TRACE_KEY = "traceId";

    /**
     * Defines the execution phase of the filter within the Netty pipeline.
     * Enforces execution at the earliest possible phase (TRACING) before security or routing logic.
     *
     * @return Integer representing the filter order phase.
     */
    @Override
    public int getOrder() {
        return ServerFilterPhase.TRACING.order();
    }

    /**
     * Intercepts inbound HTTP requests to extract or generate a unique correlation Trace ID,
     * binding it to SLF4J MDC and propagating it across reactive thread hops.
     *
     * @param request The incoming HTTP request. Must not be null.
     * @param chain   The server filter execution chain. Must not be null.
     * @return A Publisher emitting the mutable HTTP response with reactive context propagation.
     */
    @Override
    @NonNull
    public Publisher<MutableHttpResponse<?>> doFilter(@NonNull HttpRequest<?> request, @NonNull ServerFilterChain chain) {
        String traceId = request.getHeaders().get(TRACE_ID_HEADER);

        // Fail-safe: Automatically mints a new UUID if the upstream client lacks tracing instrumentation
        if (traceId == null || traceId.isBlank()) {
            traceId = UUID.randomUUID().toString();
            log.trace("[TELEMETRY] Missing Trace ID in inbound request. Generated fresh UUID: {}", traceId);
        }

        final String finalTraceId = traceId;

        // Binds the Trace ID to the reactive chain, ensuring MDC persistence across thread transitions
        return Mono.defer(() -> {
            MDC.put(MDC_TRACE_KEY, finalTraceId);
            request.setAttribute(MDC_TRACE_KEY, finalTraceId);

            return Mono.from(chain.proceed(request))
                    .doFinally(signalType -> MDC.remove(MDC_TRACE_KEY));
        }).contextWrite(Context.of(MDC_TRACE_KEY, finalTraceId));
    }
}