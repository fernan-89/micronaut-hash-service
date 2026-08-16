package com.thinklab.infrastructure.health;

import io.micronaut.context.event.ApplicationEventListener;
import io.micronaut.context.event.ShutdownEvent;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;

import java.net.InetAddress;
import java.util.Objects;

/**
 * Infrastructure Component: Intercepts application termination to provide structured shutdown telemetry.
 *
 * <p><b>Architectural Role:</b>
 * Ensures that the application lifecycle logs maintain strict SRE observability standards even during
 * graceful shutdowns, preventing blank or `NONE` telemetry markers when container orchestrators
 * (e.g., Kubernetes) issue a SIGTERM signal.
 *
 * <p><b>Execution Context Forensics:</b>
 * Injects pre-flight MDC states (`SYSTEM-SHUTDOWN`) onto the termination threads, capturing the node's
 * IP footprint and shutdown initiator context.
 *
 * @author Thinklab Systems Engineering Team
 * @version 1.1.0-NASA-SRE
 * @since 1.0
 */
@Singleton
@Slf4j
public class ApplicationShutdownObserver implements ApplicationEventListener<ShutdownEvent> {

    private final String infrastructureIpAddress;

    /**
     * Initializes the shutdown observer and caches local networking context.
     */
    public ApplicationShutdownObserver() {
        this.infrastructureIpAddress = resolveIpAddress();
    }

    /**
     * Intercepts the framework's ShutdownEvent to emit structured telemetry prior to container teardown.
     *
     * @param event The application shutdown event triggered by the IoC container. Must not be null.
     */
    @Override
    public void onApplicationEvent(ShutdownEvent event) {
        Objects.requireNonNull(event, "Application constraint violated: ShutdownEvent cannot be null.");

        // Inject SRE Forensics context into the shutdown thread MDC
        injectShutdownContext();

        log.warn("[APPLICATION_SHUTDOWN] 🛑 Graceful teardown signal received. Draining connection pools and unregistering from routing mesh...");
    }

    /**
     * Asserts the System Shutdown context into the active thread's MDC.
     */
    private void injectShutdownContext() {
        MDC.put("traceId", "SYSTEM-SHUTDOWN");
        MDC.put("clientIp", this.infrastructureIpAddress);
        MDC.put("userAgent", "Micronaut-Engine/Shutdown");
        // Fallbacks for Logback layout compatibility
        MDC.put("ip", this.infrastructureIpAddress);
        MDC.put("client", "Micronaut-Engine/Shutdown");
    }

    /**
     * Resolves the local network IP address safely.
     */
    private String resolveIpAddress() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            return "127.0.0.1";
        }
    }
}