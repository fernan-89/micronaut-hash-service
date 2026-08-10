package com.thinklab;

import com.thinklab.infrastructure.telemetry.ReactorMdcBridge;
import io.micronaut.runtime.Micronaut;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Hooks;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import java.security.Security;
import java.util.TimeZone;

/**
 * Main Entry Point: Bootstrap class for the Thinklab Hash Service.
 *
 * <p><b>Architectural Role:</b>
 * This class orchestrates the application bootstrap sequence using the Micronaut framework, ensuring
 * high scalability, low memory footprint, and non-blocking asynchronous execution. It also establishes
 * critical JVM-level security, time-drift, and reactive context-propagation invariants before the
 * inversion of control (IoC) container boots.
 *
 * <p><b>Contractual Obligations:</b>
 * <ul>
 * <li><b>Universal UTC Invariant:</b> Forcefully normalizes the JVM default timezone to UTC to prevent
 *     distributed time drift across cryptographic token lifecycles.</li>
 * <li><b>Reactive Context Propagation (MDC Bridge):</b> Enables automatic Project Reactor hooks and MDC bridges
 *     to inherit Mapped Diagnostic Context states across non-blocking asynchronous thread boundaries.</li>
 * <li><b>Unhandled Exception Guard:</b> Configures a global uncaught exception handler to intercept
 *     catastrophic thread deaths bypassing reactive contexts.</li>
 * <li><b>Cryptographic Provider Injection:</b> Registers the Bouncy Castle security provider to satisfy
 *     advanced hashing algorithm dependencies.</li>
 * <li><b>Fail-Fast Bootstrapping:</b> Intercepts container initialization failures and exits cleanly with
 *     a non-zero status code for orchestration mesh awareness.</li>
 * </ul>
 *
 * @author ThinkLab
 * @version 1.2.0
 * @since 1.0
 */
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

    /**
     * Main method to launch the Micronaut runtime and enforce global JVM baseline constraints.
     *
     * @param args Command line arguments passed during startup. Must not be null.
     */
    public static void main(String[] args) {
        // 1. Mission Critical: Enforce UTC globally to prevent time-drift bugs in distributed tokens
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));

        // 2. Mission Critical: Enable automatic MDC propagation hooks & Reactor-to-MDC bridge
        Hooks.enableAutomaticContextPropagation();
        ReactorMdcBridge.register();

        // 3. Mission Critical: Catch catastrophic thread deaths that bypass the Reactive context
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) ->
                log.error("[JVM_FATAL] - Unhandled exception in thread [{}]: {}", thread.getName(), throwable.getMessage(), throwable)
        );

        // 4. Mission Critical: Inject Bouncy Castle to satisfy native JVM cryptographic dependencies
        Security.addProvider(new BouncyCastleProvider());

        // 5. Boot: Use the Builder for explicit control over startup arguments and container lifecycle
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