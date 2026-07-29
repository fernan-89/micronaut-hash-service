package com.thinklab;

import io.micronaut.runtime.Micronaut;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import java.security.Security;
import java.util.TimeZone;

// ==============================================================================================
/**
 * Main Entry Point: Bootstrap class for the Thinklab Hash Service.
 * <p>
 * This class orchestrates the application startup using the Micronaut framework,
 * ensuring high scalability, low memory footprint, and non-blocking execution.
 * It also establishes critical JVM-level invariants before the IoC container boots.
 * </p>
 *
 * <b>Architectural Role:</b>
 * <ul>
 *     <li><b>Bootstrap:</b> Triggers the dependency injection container and Netty server.</li>
 *     <li><b>API Documentation:</b> Defines global OpenAPI 3.0 metadata for Swagger.</li>
 *     <li><b>Component Scan Root:</b> Discovers all sub-modules (domain, application, infra).</li>
 * </ul>
 *
 * <b>Invariant:</b> The JVM TimeZone is strictly forced to UTC to prevent distributed time drift.
 *
 * @module      Thinklab Infrastructure / Core
 * @maintainer  Thinklab Systems Engineering Team
 * @version     1.0.0
 */
// ==============================================================================================
@OpenAPIDefinition(
        info = @Info(
                title = "hash-&-serial-registry",
                version = "1.0.0",
                description = "High-performance reactive service for generating, auditing, and managing the lifecycle of cryptographic tokens.",
                contact = @Contact(name = "Thinklab Staff Engineering", email = "staff@thinklab.com"),
                license = @License(name = "Apache 2.0", url = "https://thinklab.com/licenses/LICENSE-2.0")
        )
)
public class Application {

    private static final Logger log = LoggerFactory.getLogger(Application.class);

    // ----------------------------------------------------------------------------------------------
    /**
     * Main method to launch the Micronaut runtime and enforce JVM constraints.
     *
     * @param args Command line arguments passed during startup.
     * @pre    The JVM has allocated the required heap space and OS file descriptors.
     * @post   The Netty reactive web server is bound to the configured port, accepting traffic.
     */
    // ----------------------------------------------------------------------------------------------
    public static void main(String[] args) {

        // 1. Mission Critical: Enforce UTC globally to prevent time-drift bugs in distributed tokens
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));

        // 2. Mission Critical: Catch catastrophic thread deaths that bypass the Reactive context
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) ->
                log.error("[JVM_FATAL] - Unhandled exception in thread [{}]: {}", thread.getName(), throwable.getMessage(), throwable)
        );

        // 3. Mission Critical: Inject Bouncy Castle to satisfy native JVM BLAKE3 dependencies
        Security.addProvider(new BouncyCastleProvider());

        // 4. Boot: Use the Builder for explicit control over startup arguments
        try {
            Micronaut.build(args)
                    .mainClass(Application.class)
                    .start();
        } catch (Exception bootException) {
            log.error("[BOOT_FAILED] - Application container failed to initialize: {}", bootException.getMessage(), bootException);
            System.exit(1);
        }
    }
}