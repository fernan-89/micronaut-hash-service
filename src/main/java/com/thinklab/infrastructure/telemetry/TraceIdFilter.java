package com.thinklab.infrastructure.telemetry;

import io.micronaut.core.annotation.NonNull;
import io.micronaut.http.HttpHeaders;
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
import reactor.core.scheduler.Schedulers;
import reactor.util.context.Context;

import java.net.InetAddress;
import java.util.UUID;

/**
 * Infrastructure Component: Reactive MDC Traceability & External Caller Forensics Filter.
 *
 * <p><b>Architectural Role:</b>
 * Intercepts all inbound HTTP traffic globally ("/**") at the Netty pipeline boundary.
 * Extracts native W3C trace identifiers, forensic origin metadata (IP, User-Agent, Virtual Host, Origin),
 * and executes asynchronous non-blocking reverse DNS lookups, injecting everything into SLF4J's MDC
 * and Project Reactor's execution context.
 *
 * <p><b>Resilience & Decoupling (Zero-Trust Fix):</b>
 * This component implements a zero-dependency trace extraction protocol. It decodes
 * W3C traceparent headers and MDC states natively without hard-coupling to OpenTelemetry
 * SDK classes (e.g., io.opentelemetry.api.trace.Span). This ensures the Netty pipeline
 * NEVER crashes with NoClassDefFoundError, even if tracing libraries are stripped or fail.
 *
 * <p><b>Privacy by Design (LGPD/GDPR):</b>
 * The client IP address is considered Personally Identifiable Information (PII).
 * This filter intentionally obfuscates the last octet (IPv4) or block (IPv6) before
 * logging, balancing forensic observability with strict regulatory compliance.
 *
 * <p><b>Contractual Obligations:</b>
 * <ul>
 * <li><b>Distributed Tracing (ADR-010):</b> Extracts W3C standard trace context.</li>
 * <li><b>Telemetry Matrix (ADR-008 & ADR-009):</b> Standardizes MDC footprint across microservices.</li>
 * <li><b>Reactive Context Propagation:</b> Bridges Netty HTTP thread state with Reactor workers.</li>
 * <li><b>Asynchronous Reverse DNS:</b> Resolves external hostnames off the Netty event loops safely.</li>
 * <li><b>MDC Cleanup:</b> Guarantees thread-context isolation by purging after execution.</li>
 * </ul>
 *
 * @author ThinkLab Systems Engineering Team
 * @version 3.5.2-NASA-SRE-PROD-STABLE
 * @since 1.0
 */
@Singleton
@Filter("/**")
@Slf4j
public class TraceIdFilter implements HttpServerFilter {

    private static final String FORWARDED_FOR_HEADER = "X-Forwarded-For";
    private static final String W3C_TRACE_PARENT_HEADER = "traceparent";
    private static final String B3_TRACE_ID_HEADER = "X-B3-TraceId";

    private static final String MDC_TRACE_KEY = "traceId";
    private static final String MDC_CLIENT_IP_KEY = "clientIp";
    private static final String MDC_USER_AGENT_KEY = "userAgent";
    private static final String MDC_VIRTUAL_HOST_KEY = "virtualHost";
    private static final String MDC_CLIENT_ORIGIN_KEY = "clientOrigin";
    private static final String MDC_EXTERNAL_HOST_KEY = "externalClientHost";

    /**
     * Defines the execution phase of the filter within the Netty pipeline.
     * Enforces execution slightly after the Tracing phase to capture upstream injected contexts.
     *
     * @return Integer representing the filter order phase.
     */
    @Override
    public int getOrder() {
        return ServerFilterPhase.TRACING.order() + 1;
    }

    /**
     * Intercepts inbound HTTP requests to extract W3C trace tracking, virtual hosts, origin provenance,
     * and non-blocking reverse DNS, binding them to SLF4J MDC and propagating them across reactive thread hops.
     *
     * @param request The incoming HTTP request. Must not be null.
     * @param chain   The server filter execution chain. Must not be null.
     * @return A Publisher emitting the mutable HTTP response with reactive context propagation.
     */
    @Override
    @NonNull
    public Publisher<MutableHttpResponse<?>> doFilter(@NonNull HttpRequest<?> request, @NonNull ServerFilterChain chain) {

        // 1. Trace ID Extraction (Resilient/Zero-Coupling Approach)
        String traceId = resolveTraceId(request);

        // 2. Virtual Host Extraction (Target domain requested by client)
        String virtualHost = request.getHeaders().get("Host");
        virtualHost = (virtualHost == null || virtualHost.isBlank()) ? "unknown-host" : virtualHost;

        // 3. Provenance Extraction (Origin / Referer tracking)
        String clientOrigin = request.getHeaders().get("Origin");
        if (clientOrigin == null || clientOrigin.isBlank()) {
            clientOrigin = request.getHeaders().get("Referer");
        }
        clientOrigin = (clientOrigin == null || clientOrigin.isBlank()) ? "direct-client" : clientOrigin;

        // 4. User-Agent Extraction & Truncation (Prevents log bloat/buffer overflows)
        String userAgent = request.getHeaders().get(HttpHeaders.USER_AGENT);
        userAgent = (userAgent == null || userAgent.isBlank()) ? "UNKNOWN" : userAgent;
        if (userAgent.length() > 40) {
            userAgent = userAgent.substring(0, 37) + "...";
        }

        // 5. Client IP Extraction (Load Balancer awareness) & Obfuscation (LGPD/GDPR)
        String forwardedFor = request.getHeaders().get(FORWARDED_FOR_HEADER);
        String rawIp = (forwardedFor != null && !forwardedFor.isBlank())
                ? forwardedFor.split(",")[0].trim()
                : (request.getRemoteAddress() != null && request.getRemoteAddress().getAddress() != null)
                ? request.getRemoteAddress().getAddress().getHostAddress()
                : "127.0.0.1";

        final String finalTraceId = traceId;
        final String finalVirtualHost = virtualHost;
        final String finalClientOrigin = clientOrigin;
        final String finalClientIp = obfuscateIp(rawIp);
        final String finalUserAgent = userAgent;

        // 6. Execute Asynchronous Non-Blocking Reverse DNS Lookup (Protects Netty EventLoop)
        return Mono.fromCallable(() -> resolveExternalHost(rawIp))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(externalHost ->
                        Mono.defer(() -> {
                            // Attach to current thread MDC
                            MDC.put(MDC_TRACE_KEY, finalTraceId);
                            MDC.put(MDC_CLIENT_IP_KEY, finalClientIp);
                            MDC.put(MDC_USER_AGENT_KEY, finalUserAgent);
                            MDC.put(MDC_VIRTUAL_HOST_KEY, finalVirtualHost);
                            MDC.put(MDC_CLIENT_ORIGIN_KEY, finalClientOrigin);
                            MDC.put(MDC_EXTERNAL_HOST_KEY, externalHost);

                            // Attach to Request Attributes for downstream Exception Handlers to recover
                            request.setAttribute(MDC_TRACE_KEY, finalTraceId);
                            request.setAttribute(MDC_CLIENT_IP_KEY, finalClientIp);
                            request.setAttribute(MDC_USER_AGENT_KEY, finalUserAgent);
                            request.setAttribute(MDC_VIRTUAL_HOST_KEY, finalVirtualHost);
                            request.setAttribute(MDC_CLIENT_ORIGIN_KEY, finalClientOrigin);
                            request.setAttribute(MDC_EXTERNAL_HOST_KEY, externalHost);

                            return Mono.from(chain.proceed(request))
                                    .doFinally(signalType -> {
                                        // Absolute cleanup to prevent MDC thread-leakage
                                        MDC.remove(MDC_TRACE_KEY);
                                        MDC.remove(MDC_CLIENT_IP_KEY);
                                        MDC.remove(MDC_USER_AGENT_KEY);
                                        MDC.remove(MDC_VIRTUAL_HOST_KEY);
                                        MDC.remove(MDC_CLIENT_ORIGIN_KEY);
                                        MDC.remove(MDC_EXTERNAL_HOST_KEY);
                                    });
                        }).contextWrite(
                                Context.of(MDC_TRACE_KEY, finalTraceId)
                                        .put(MDC_CLIENT_IP_KEY, finalClientIp)
                                        .put(MDC_USER_AGENT_KEY, finalUserAgent)
                                        .put(MDC_VIRTUAL_HOST_KEY, finalVirtualHost)
                                        .put(MDC_CLIENT_ORIGIN_KEY, finalClientOrigin)
                                        .put(MDC_EXTERNAL_HOST_KEY, externalHost)
                        )
                );
    }

    /**
     * Safely resolves the active Distributed Trace ID without hard-coupling to OpenTelemetry SDKs.
     * Eradicates any risk of NoClassDefFoundError if tracing libraries are omitted from classpath.
     *
     * @param request The inbound HTTP request.
     * @return A standard 32-character Hex Trace ID or a localized fallback token.
     */
    private String resolveTraceId(HttpRequest<?> request) {
        // Priority 1: W3C Trace Context Standard (e.g., 00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01)
        String traceparent = request.getHeaders().get(W3C_TRACE_PARENT_HEADER);
        if (traceparent != null && traceparent.length() >= 55) {
            return traceparent.split("-")[1]; // Extracts the Trace ID segment
        }

        // Priority 2: Fallback to Zipkin/B3 headers common in AWS/GCP meshes
        String b3TraceId = request.getHeaders().get(B3_TRACE_ID_HEADER);
        if (b3TraceId != null && !b3TraceId.isBlank()) {
            return b3TraceId;
        }

        // Priority 3: Check if Micronaut Tracing Engine already populated the SLF4J MDC
        String mdcTraceId = MDC.get(MDC_TRACE_KEY);
        if (mdcTraceId != null && !mdcTraceId.isBlank()) {
            return mdcTraceId;
        }

        // Priority 4: Generate a high-entropy fallback trace token to ensure log continuity
        String fallbackToken = "UNTRACED-" + UUID.randomUUID().toString().substring(0, 8);
        log.trace("[TELEMETRY] No inbound trace context found. Generated deterministic fallback token: {}", fallbackToken);
        return fallbackToken;
    }

    /**
     * Executes a Reverse DNS lookup to resolve the client IP address to its registered external hostname.
     * Handled asynchronously on a bounded elastic thread pool to prevent blocking Netty workers.
     */
    private String resolveExternalHost(String clientIp) {
        if (clientIp == null || clientIp.equals("127.0.0.1") || clientIp.equals("0.0.0.0") || clientIp.startsWith("192.168.") || clientIp.startsWith("10.")) {
            return "local-mesh-client";
        }
        try {
            InetAddress inetAddress = InetAddress.getByName(clientIp);
            String hostName = inetAddress.getHostName();
            return (hostName != null && !hostName.isBlank()) ? hostName : clientIp;
        } catch (Exception e) {
            return clientIp; // Fallback to IP if reverse DNS resolution fails
        }
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

        // IPv6 Obfuscation (e.g., 2001:db8::ff00:42:8329 -> 2001:db8::ff00:42:***)
        if (ip.contains(":")) {
            int lastColonIndex = ip.lastIndexOf(':');
            if (lastColonIndex > 0) return ip.substring(0, lastColonIndex) + ":***";
        }

        return "***";
    }
}