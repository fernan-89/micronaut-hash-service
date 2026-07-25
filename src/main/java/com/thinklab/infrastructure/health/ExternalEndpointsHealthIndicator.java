package com.thinklab.infrastructure.health;

import io.micronaut.context.annotation.Property;
import io.micronaut.context.event.ApplicationEventListener;
import io.micronaut.context.event.StartupEvent;
import io.micronaut.health.HealthStatus;
import io.micronaut.management.health.indicator.HealthIndicator;
import io.micronaut.management.health.indicator.HealthResult;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

// ==============================================================================================
/**
 * Integrates external dependency validation into both application startup and telemetry subsystem.
 * <p>
 * This component acts as a dual-phase resilient monitor:
 * 1. Startup (Phase 1): Eagerly warms up DNS resolution, TCP sockets, and SSL handshakes.
 * 2. Runtime (Phase 2): Continually evaluates external health for the Kubernetes Readiness Probe,
 *    isolating the pod from the routing mesh if critical outbound dependencies degrade.
 * </p>
 *
 * <b>Thread Safety:</b> Thread-safe (Stateless beyond immutable configuration state).
 * <b>Invariant:</b> The internal HTTP client timeout must strictly remain under the orchestration
 *                   probe timeout threshold to prevent cascading thread starvation.
 *
 * @module      Thinklab Infrastructure / Telemetry
 * @pattern     Eager Initialization & Custom Health Indicator (Reactive Aggregator)
 * @maintainer  Thinklab Systems Engineering Team
 * @version     1.2.0
 */
// ==============================================================================================
@Singleton
@Slf4j
public class ExternalEndpointsHealthIndicator implements HealthIndicator, ApplicationEventListener<StartupEvent> {

    // Transitioned from List<String> to Map<String, String> for logical aliasing
    @Property(name = "warmup.endpoints")
    private Map<String, String> targetEndpoints;

    /**
     * Pre-configured, reusable native HTTP client.
     * Bounded to a strict 2-second connection timeout to ensure deterministic
     * health check resolution times and prevent resource exhaustion.
     */
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(2))
            .build();

    // ----------------------------------------------------------------------------------------------
    // PHASE 1: BOOTSTRAP WARM-UP (Eager Initialization)
    // ----------------------------------------------------------------------------------------------

    /**
     * Intercepts the framework's StartupEvent to proactively initialize network paths.
     *
     * @param event The application startup event triggered by the IoC container.
     * @pre         The application context is fully initialized and configuration injected.
     * @post        DNS and SSL caches are warmed up without blocking the main startup thread.
     */
    @Override
    public void onApplicationEvent(StartupEvent event) {
        if (targetEndpoints == null || targetEndpoints.isEmpty()) {
            log.info("[EXTERNAL_HEALTH_WARMUP] - No external targets configured. Skipping warmup phase.");
            return;
        }

        log.info("[EXTERNAL_HEALTH_WARMUP] - Initiating proactive warmup for {} external dependencies...", targetEndpoints.size());

        Flux.fromIterable(targetEndpoints.entrySet())
                .flatMap(this::checkEndpoint)
                .doOnNext(entry -> log.info("[EXTERNAL_HEALTH_WARMUP] - Warmup state: [{}] -> {}", entry.getKey(), entry.getValue()))
                .subscribe();
    }

    // ----------------------------------------------------------------------------------------------
    // PHASE 2: TELEMETRY & HEALTH INDICATOR (Runtime / Orchestrator Probes)
    // ----------------------------------------------------------------------------------------------

    /**
     * Evaluates the current health state of all configured external endpoints.
     * <p>
     * Invoked periodically by Micronaut's actuator subsystem. Executes all health checks
     * concurrently via Project Reactor and aggregates the results into a unified topology state.
     * </p>
     *
     * @return A Publisher emitting the aggregated health status and diagnostic map.
     *         Evaluates in finite time bound by the underlying HttpClient configuration.
     */
    @Override
    public Publisher<HealthResult> getResult() {
        if (targetEndpoints == null || targetEndpoints.isEmpty()) {
            return Mono.just(HealthResult.builder("external-endpoints")
                    .status(HealthStatus.UP)
                    .details("No external endpoints configured for telemetry.")
                    .build());
        }

        return Flux.fromIterable(targetEndpoints.entrySet())
                .flatMap(this::checkEndpoint)
                .collectMap(Map.Entry::getKey, Map.Entry::getValue)
                .map(details -> {
                    // Strict evaluation: Any degraded outbound dependency cascades to a DOWN state.
                    boolean hasFailures = details.values().stream()
                            .anyMatch(status -> status.toString().startsWith("DOWN"));

                    HealthStatus aggregatedStatus = hasFailures ? HealthStatus.DOWN : HealthStatus.UP;

                    return HealthResult.builder("external-endpoints")
                            .status(aggregatedStatus)
                            .details(details)
                            .build();
                });
    }

    // ----------------------------------------------------------------------------------------------
    // SHARED INFRASTRUCTURE: DETERMINISTIC NETWORK PROBING
    // ----------------------------------------------------------------------------------------------

    /**
     * Dispatches a deterministic, non-blocking HTTP GET request to the specified target.
     * <p>
     * Discards the HTTP response body immediately to enforce an O(1) memory footprint
     * per request. Safely handles and wraps network/DNS exceptions to maintain reactive stream integrity.
     * </p>
     *
     * @param endpointDefinition A Key-Value pair representing the logical alias and the target URI.
     * @return A Mono emitting a Key-Value pair containing the logical alias and its resolved
     *         health state. Never emits an error signal.
     */
    private Mono<Map.Entry<String, String>> checkEndpoint(Map.Entry<String, String> endpointDefinition) {
        String alias = endpointDefinition.getKey();
        String url = endpointDefinition.getValue();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .timeout(Duration.ofSeconds(3)) // Absolute timeout per request
                .build();

        return Mono.fromFuture(httpClient.sendAsync(request, HttpResponse.BodyHandlers.discarding()))
                .map(response -> {
                    boolean isUp = response.statusCode() >= 200 && response.statusCode() < 400;
                    String statusMessage = isUp ? "UP" : "DOWN (HTTP " + response.statusCode() + ")";

                    return (Map.Entry<String, String>) Map.entry(alias, statusMessage);
                })
                .onErrorResume(error -> {
                    log.debug("[EXTERNAL_HEALTH_PROBE] - Diagnostic failure for [{}] ({}) - Reason: {}", alias, url, error.getMessage());
                    return Mono.just((Map.Entry<String, String>) Map.entry(alias, "DOWN (" + error.getMessage() + ")"));
                });
    }
}