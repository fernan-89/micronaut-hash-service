# Thinklab Hash Service

## Overview
The Thinklab Hash Service is a mission-critical, high-assurance microservice engineered for the authoritative management of the complete lifecycle of cryptographic hash tokens. Developed using Java 21 and the Micronaut Framework, this service utilizes Hexagonal Architecture (Ports and Adapters) combined with a fully Reactive Stack to ensure high throughput, zero-blocking I/O, and absolute structural maintainability for enterprise-scale identity and security ecosystems.

## Technology Stack
* Runtime: Java 21 LTS (Amazon Corretto / GraalVM)
* Framework: Micronaut Framework 4.4.2 (AOT Optimized)
* Reactive Engine: Project Reactor (Mono / Flux)
* Persistence: Reactive MongoDB utilizing BSON Binary UUID Subtype 4 for optimized indexing
* Testing Suite: JUnit 5, Mockito (Unit), and Testcontainers (Integration)
* Documentation: OpenAPI 3.0 / Swagger (Generated at compile-time via AOT)

## Architectural Model
The project implements a strict three-tier hexagonal structure to ensure that the Core Domain remains framework-agnostic and immune to infrastructure volatility:

src/main/java/com/thinklab/
├── domain/                  # Core Business Logic (Rich Models, Value Objects, Pure Exceptions)
├── application/             # Orchestration & Use Cases (Input/Output Ports, Interactors)
└── infrastructure/          # External Integrations (REST Controllers, MongoDB Adapters, Configs)

## Engineering Mandates and Core Features

### Identity Sovereignty (ADR 005)
Mandatory enforcement of native java.util.UUID for all primary and correlation identifiers. Identity generation is governed by deterministic seeds to ensure idempotency across distributed nodes and optimal database performance.

### Forensic Audit Trail (ADR 003)
Every state mutation—including HASH_GENERATION, DEACTIVATION, REACTIVATION, and REVOCATION—is captured within an immutable, append-only forensic ledger to ensure absolute accountability and regulatory compliance.

### Defensive Engineering
Strict nullability controls and compact record constructors are utilized to protect domain invariants at the point of instantiation. Boundary validation is further reinforced through Jakarta JSR-380 annotations within DTOs and Command objects.

### Parallel CQRS Projections
Implementation of the 360-degree view pattern, which orchestrates parallel retrieval of entity state and forensic audit logs using reactive composition (Mono.zip). This reduces API latency and ensures high-fidelity data discovery.

### Resilient Exception Boundaries
Structural decoupling of Business Exceptions from Infrastructure Failures. All errors are projected into standardized RFC 7807 (Problem Details) payloads, maintaining a consistent contract for API consumers.

### Observability and Telemetry
Standardized logging context follows the [ACTION: NAME] [ID: UUID] pattern. The service includes integrated Prometheus metrics, as well as native Kubernetes Liveness and Readiness probes.

## Operational Procedures

### Build and Run
Ensure the local environment is aligned with the required Java 21 toolchain before execution.

# Clean, compile with AOT optimizations, and Build
./gradlew clean build --refresh-dependencies

# Start the Reactive Service (Default Port: 8080)
./gradlew run

### Testing Execution
The suite follows the automated test pyramid to ensure absolute reliability across business logic and infrastructure adapters.

# Execute all Unit and Integration tests
./gradlew test

### Infrastructure and API Endpoints
* Health and Readiness: http://localhost:8080/health
* Swagger UI: http://localhost:8080/swagger/views/swagger-ui
* OpenAPI Specs: http://localhost:8080/swagger/thinklab-hash-service-1.0.0.yml

### E2E Testing (Postman)
A complete "Hash Lifecycle Suite" is available for Postman, including automated scripts (pm.test) to validate structural integrity and state machine transitions across the entire token lifecycle.