package com.thinklab.infrastructure.telemetry;

import io.micronaut.core.annotation.NonNull;
import io.micronaut.http.HttpHeaders;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.MutableHttpResponse;
import io.micronaut.http.annotation.Filter;
import io.micronaut.http.filter.HttpServerFilter;
import io.micronaut.http.filter.ServerFilterChain;
import io.micronaut.http.filter.ServerFilterPhase;
import io.opentelemetry.api.trace.Span;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Publisher;
import org.slf4j.MDC;
import reactor.core.publisher.Mono;
import reactor.util.context.Context;

/**
 * Infrastructure Component: Reactive MDC Traceability & Origin Filter.
 *
 * <p><b>Architectural Role:</b>
 * Intercepts all inbound HTTP traffic globally ("/**") at the Netty pipeline boundary.
 * Extracts native OpenTelemetry W3C trace identifiers and forensic origin metadata (IP, User-Agent),
 * injecting them into SLF4J's MDC and Project Reactor's execution context.
 *
 * <p><b>Privacy by Design (LGPD/GDPR):</b>
 * The client IP address is considered Personally Identifiable Information (PII).
 * This filter intentionally obfuscates the last octet (IPv4) or block (IPv6) before
 * logging, balancing forensic observability with strict regulatory compliance.
 *
 * <p><b>Contractual Obligations:</b>
 * <ul>
 * <li><b>OpenTelemetry W3C (ADR-010):</b> Extracts the active global distributed span trace ID instead of proprietary custom headers.</li>
 * <li><b>Telemetry Matrix (ADR-008 & ADR-009):</b> Establishes the baseline traceability matrix (traceId, clientIp, userAgent) for all downstream components.</li>
 * <li><b>Reactive Context Propagation:</b> Bridges Netty HTTP thread state with Project Reactor workers to ensure distributed tracing continuity.</li>
 * <li><b>MDC Cleanup:</b> Guarantees thread-context isolation by purging MDC attributes upon request finalization.</li>
 * </ul>
 *
 * @author ThinkLab
 * @version 3.0.0
 * @since 1.0
 */
@Singleton
@Filter("/**")
@Slf4j
public class TraceIdFilter implements HttpServerFilter {

    private static final String FORWARDED_FOR_HEADER = "X-Forwarded-For";

    private static final String MDC_TRACE_KEY = "traceId";
    private static final String MDC_CLIENT_IP_KEY = "clientIp";
    private static final String MDC_USER_AGENT_KEY = "userAgent";

    /**
     * Defines the execution phase of the filter within the Netty pipeline.
     * Enforces execution slightly after the OpenTelemetry tracing phase to ensure valid span contexts.
     *
     * @return Integer representing the filter order phase.
     */
    @Override
    public int getOrder() {
        return ServerFilterPhase.TRACING.order() + 1;
    }

    /**
     * Intercepts inbound HTTP requests to extract OpenTelemetry W3C trace tracking and origin metadata,
     * binding them to SLF4J MDC and propagating them across reactive thread hops.
     *
     * @param request The incoming HTTP request. Must not be null.
     * @param chain   The server filter execution chain. Must not be null.
     * @return A Publisher emitting the mutable HTTP response with reactive context propagation.
     */
    @Override
    @NonNull
    public Publisher<MutableHttpResponse<?>> doFilter(@NonNull HttpRequest<?> request, @NonNull ServerFilterChain chain) {
        // 1. Trace ID Extraction (Native OpenTelemetry W3C Span Context)
        String traceId = Span.current().getSpanContext().getTraceId();
        if (traceId == null || traceId.isBlank() || traceId.equals("00000000000000000000000000000000")) {
            traceId = "UNTRACED-" + java.util.UUID.randomUUID().toString().substring(0, 8);
            log.trace("[TELEMETRY] Active OpenTelemetry span missing or invalid. Fallback trace token generated: {}", traceId);
        }

        // 2. User-Agent Extraction & Truncation (Prevents log bloat from malicious long headers)
        String userAgent = request.getHeaders().get(HttpHeaders.USER_AGENT);
        userAgent = (userAgent == null || userAgent.isBlank()) ? "UNKNOWN" : userAgent;
        if (userAgent.length() > 40) {
            userAgent = userAgent.substring(0, 37) + "...";
        }

        // 3. Client IP Extraction (Load Balancer awareness) & Obfuscation (LGPD)
        String forwardedFor = request.getHeaders().get(FORWARDED_FOR_HEADER);
        String rawIp = (forwardedFor != null && !forwardedFor.isBlank())
                ? forwardedFor.split(",")[0].trim()
                : request.getRemoteAddress().getHostString();

        final String finalTraceId = traceId;
        final String finalClientIp = obfuscateIp(rawIp);
        final String finalUserAgent = userAgent;

        // Binds the telemetry matrix to the reactive chain and request attributes (for Exception Handlers)
        return Mono.defer(() -> {
            MDC.put(MDC_TRACE_KEY, finalTraceId);
            MDC.put(MDC_CLIENT_IP_KEY, finalClientIp);
            MDC.put(MDC_USER_AGENT_KEY, finalUserAgent);

            // Saves to attributes so GlobalExceptionHandler can retrieve them if the reactive stream drops
            request.setAttribute(MDC_TRACE_KEY, finalTraceId);
            request.setAttribute(MDC_CLIENT_IP_KEY, finalClientIp);
            request.setAttribute(MDC_USER_AGENT_KEY, finalUserAgent);

            return Mono.from(chain.proceed(request))
                    .doFinally(signalType -> {
                        MDC.remove(MDC_TRACE_KEY);
                        MDC.remove(MDC_CLIENT_IP_KEY);
                        MDC.remove(MDC_USER_AGENT_KEY);
                    });
        }).contextWrite(Context.of(
                MDC_TRACE_KEY, finalTraceId,
                MDC_CLIENT_IP_KEY, finalClientIp,
                MDC_USER_AGENT_KEY, finalUserAgent
        ));
    }

    /**
     * Masks the final segment of an IP address to comply with data privacy regulations (LGPD/GDPR).
     *
     * @param ip The raw IP address (IPv4 or IPv6).
     * @return The obfuscated IP address string.
     */
    private String obfuscateIp(String ip) {
        if (ip == null || ip.isBlank()) return "UNKNOWN";

        // IPv4 Obfuscation (e.g., 192.168.1.150 -> 192.168.1.***)
        if (ip.contains(".")) {
            int lastDotIndex = ip.lastIndexOf('.');
            if (lastDotIndex > 0) return ip.substring(0, lastDotIndex) + ".***";
        }

        // IPv6 Obfuscation
        if (ip.contains(":")) {
            int lastColonIndex = ip.lastIndexOf(':');
            if (lastColonIndex > 0) return ip.substring(0, lastColonIndex) + ":***";
        }

        return "***";
    }
}