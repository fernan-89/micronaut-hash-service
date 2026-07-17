package com.thinklab;

import io.micronaut.runtime.Micronaut;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;

/**
 * Main Entry Point: Bootstrap class for the Thinklab Hash Service.
 * This class orchestrates the application startup using the Micronaut framework,
 * ensuring high scalability, low memory footprint, and non-blocking execution.
 *
 * <p><b>Architectural Role:</b></p>
 * <ul>
 *     <li><b>Bootstrap:</b> Triggers the dependency injection container and Netty server.</li>
 *     <li><b>API Documentation:</b> Defines the global OpenAPI 3.0 metadata for Swagger generation.</li>
 *     <li><b>Component Scan Root:</b> Must reside in the root package to ensure all
 *         sub-modules (domain, application, infrastructure) are discovered.</li>
 * </ul>
 *
 * @author Thinklab Engineering
 * @version 1.0.0
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

    /**
     * Main method to launch the Micronaut runtime.
     *
     * @param args Command line arguments passed during startup.
     */
    public static void main(String[] args) {
        Micronaut.run(Application.class, args);
    }
}