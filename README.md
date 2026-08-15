# Thinklab Hash Service

## Overview

The Thinklab Hash Service is a mission-critical, high-assurance microservice engineered for the authoritative management of the complete lifecycle of cryptographic hash tokens. Developed using Java 21 and the Micronaut Framework, this service utilizes Hexagonal Architecture (Ports and Adapters) combined with a fully Reactive Stack to ensure high throughput, zero-blocking I/O, and absolute structural maintainability for enterprise-scale identity and security ecosystems.

## Technology Stack

* **Runtime:** Java 21 LTS (Amazon Corretto / GraalVM)


* **Framework:** Micronaut Framework 4.4.2 (AOT Optimized & Micronaut Serde for reflection-free JSON)


* **Reactive Engine:** Project Reactor (Mono / Flux)


* **Persistence:** Reactive MongoDB utilizing BSON Binary UUID Subtype 4 for optimized indexing


* **Observability:** SLF4J, Logback (Async), Project Reactor Hooks, and LogstashEncoder


* **Testing Suite:** JUnit 5, Mockito (Unit), and Testcontainers (Integration)


* **Documentation:** OpenAPI 3.0 / Swagger (Generated at compile-time via AOT)



---

## Architectural Model & Engineering Mandates

The project implements a strict three-tier hexagonal structure to ensure that the Core Domain remains framework-agnostic and immune to infrastructure volatility:

```text
src/main/java/com/thinklab/
├── domain/                  # Core Business Logic (Rich Models, Value Objects, Pure Exceptions)
├── application/             # Orchestration & Use Cases (Input/Output Ports, Interactors)
└── infrastructure/          # External Integrations (REST Controllers, MongoDB Adapters, Configs)

```

### 1. Hexagonal & DTO Isolation Pattern (ADR-001, ADR-003, ADR-004)

The Core Domain is completely decoupled from web and persistence layers.

* **Domain Purity:** Domain records are devoid of `@Serdeable`, `@Schema`, or `@MappedEntity` annotations.


* **Projection Mapping:** All input/output crosses the boundary via explicit Data Transfer Objects (DTOs) utilizing static factory transformations (e.g., `fromDomain()`).


* **Resilient Exception Boundaries:** Structural decoupling of Business Exceptions from Infrastructure Failures. All errors are projected into standardized **RFC 7807 (Problem Details)** payloads via a unified `GlobalExceptionHandler`.



### 2. Identity Sovereignty & Partial State Mutations (ADR-002, ADR-005)

* **UUID Standardization:** Mandatory enforcement of native `java.util.UUID` for all primary and correlation identifiers to ensure optimal MongoDB BSON indexing and prevent type contamination.


* **Explicit Partial Updates:** Monolithic `.save()` operations are strictly reserved for aggregate creation. State transitions (e.g., Deactivate, Revoke) utilize targeted `$set` query updates (`@Id` bounded) via custom repository ports to eliminate unique ID collisions and optimize I/O.



### 3. Proactive Initialization & Synchronous Barriers (ADR-006, ADR-007)

The application adheres to a "Fail-Fast before Port Binding" philosophy to protect Kubernetes routing.

* **Two-Phase Warmup:** A `StartupEvent` triggers a deterministic MongoDB ping to both the `admin` and dynamically parsed application databases, forcing SDAM topology discovery.


* **Synchronous Barriers:** The initialization thread is intentionally blocked (with timeouts) until all external dependencies are fully validated.


* **Passive Circuit Breaking:** If the database is unreachable, progressive backoffs are applied. If all retries exhaust, the JVM degrades gracefully and shuts down before the HTTP server ever starts, signaling Kubernetes to reschedule the pod.



### 4. Distributed Telemetry & Privacy-Preserving Logging (ADR-008, ADR-009)

End-to-end traceability across non-blocking asynchronous thread boundaries is guaranteed without violating data privacy regulations.

* **Reactive MDC Bridge:** A global Project Reactor operator hook (`ReactorMdcBridge`) intercepts every reactive signal, explicitly synchronizing the reactive `Context` with the active thread's SLF4J `MDC`. This ensures no `[trace=NONE]` logs occur during worker thread hops.


* **Privacy by Design (LGPD/GDPR):** The ingress `TraceIdFilter` captures forensic origins (`X-Forwarded-For`, `User-Agent`) but automatically obfuscates the final IP segment (e.g., `192.168.1.***`) and truncates massive User-Agents. This mitigates log-bloat and strips PII classification from observability platforms (ELK/Datadog).
* **Non-Blocking Sinks:** Logback is configured with `AsyncAppenders` to defer disk writes to the OS page cache, protecting Netty event loops from I/O starvation.



---

## Operational Procedures

### Build and Run

Ensure the local environment is aligned with the required Java 21 toolchain before execution.

```bash
# Clean, compile with AOT optimizations, and Build
./gradlew clean build --refresh-dependencies

# Start the Reactive Service (Default Port: 8080)
./gradlew run

```

### Testing Execution

The suite follows the automated test pyramid to ensure absolute reliability across business logic and infrastructure adapters.

```bash
# Execute all Unit and Integration tests
./gradlew test

```

### Infrastructure and API Endpoints

* **Health and Readiness:** `http://localhost:8080/health`

* **Swagger UI:** `http://localhost:8080/swagger/views/swagger-ui`

* **OpenAPI Specs:** `http://localhost:8080/swagger/thinklab-hash-service-1.0.0.yml`


### E2E Testing (Postman)

A complete "Hash Lifecycle Suite" is available for Postman, dynamically integrated with the global telemetry hooks.

* The collection utilizes pre-request scripts to inject a fresh `X-Trace-Id` into every call, mirroring production environments.

* Includes automated assertions (`pm.test`) to validate structural integrity, HTTP RFC 7807 problem details, and state machine transitions across the entire token lifecycle.