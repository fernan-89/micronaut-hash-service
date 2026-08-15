# ADR 011: Zero-Trust Containerization, AOT Compilation, and Resilient Telemetry

**Date:** 2026-08-15  
**Status:** Accepted & Implemented  
**Authors:** Thinklab Systems Engineering Team  
**Tags:** Architecture, Security, SRE, Build-Pipeline, Observability

---

## 1. Context & Problem Statement

As the Thinklab Hash Service progressed towards production-readiness, the engineering team encountered several critical infrastructural and runtime vulnerabilities that compromised the "NASA/SRE-Level" stability requirements:

1.  **Build Pipeline Fragility:** The reliance on third-party packaging plugins (e.g., `com.gradleup.shadow` / `johnrengelman.shadow`) introduced incompatibilities with modern Gradle execution engines (v8.x/v9.x), specifically regarding deprecated `mainClassName` properties and internal API deprecations.
2.  **Runtime Classpath Brittleness:** The global HTTP boundary filter (`TraceIdFilter`) was tightly coupled to the OpenTelemetry SDK (`io.opentelemetry.api.trace.Span`). Due to isolated class-loading dynamics and transitive dependency resolution, this resulted in fatal `java.lang.NoClassDefFoundError` exceptions upon receiving the first HTTP request, effectively crashing the request lifecycle.
3.  **Deployment Security Posture:** The lack of a formalized, Zero-Trust containerization strategy left the application theoretically vulnerable to container escapes, reverse-shells, and memory bloat.
4.  **API Governance:** The OpenAPI/Swagger documentation lacked static routing, making it inaccessible for downstream clients to discover the API contract securely.

---

## 2. Architectural Decisions

To eradicate these vulnerabilities and establish an impenetrable, high-performance execution baseline, the following strategic decisions were implemented:

### 2.1. Eradication of Legacy Fat-JAR Plugins (Micronaut AOT Adoption)
*   **Decision:** Completely removed the `shadow` plugin family from `build.gradle`.
*   **Implementation:** Delegated the application packaging entirely to the native `io.micronaut.application` and `io.micronaut.aot` (Ahead-of-Time) plugins.
*   **Rationale:** Micronaut's AOT engine performs compile-time service-loader initialization, YAML parsing, and dependency injection graph computation. This eliminates legacy Gradle property mapping errors, reduces JVM startup time (JIT phase), and guarantees deterministic, reproducible builds without third-party plugin interference.

### 2.2. Zero-Dependency Telemetry Extraction (Resilience Engineering)
*   **Decision:** Removed all static dependencies on OpenTelemetry SDKs from the application's Netty HTTP filters.
*   **Implementation:** Refactored `TraceIdFilter` to natively parse W3C standard `traceparent` and Zipkin `X-B3-TraceId` headers directly from the raw HTTP request. Implemented deterministic fallback UUID generation.
*   **Rationale:** Decoupling infrastructure boundary filters from external SDKs mathematically eliminates the possibility of `NoClassDefFoundError` at runtime. The telemetry matrix remains intact, propagating securely via Project Reactor Context and SLF4J MDC, but the pipeline is now 100% resilient to missing transitive libraries.

### 2.3. Zero-Trust Distroless Containerization & K8s Hardening
*   **Decision:** Adopted Multi-Stage BuildKit pipelines targeting Google's `distroless:nonroot` Debian 12 environment.
*   **Implementation:**
    *   **Dockerfile:** Extracts artifact compilation to an ephemeral builder stage with persistent Gradle layer caching. The final image contains *only* the JVM and the compiled artifact.
    *   **Kubernetes Manifest:** Enforces `readOnlyRootFilesystem: true`, `allowPrivilegeEscalation: false`, and drops `ALL` Linux capabilities.
*   **Rationale:** Distroless images contain no shell (`/bin/sh`), no package managers, and no utilities (`curl`, `wget`). In the event of a Remote Code Execution (RCE) vulnerability within the JVM, attackers cannot execute shell commands, download payloads, or write to the filesystem.

### 2.4. Compile-Time API Governance (Swagger UI)
*   **Decision:** Expose interactive API contracts without runtime reflection overhead.
*   **Implementation:** Configured `application.yml` to route `classpath:META-INF/swagger` to `/swagger-ui/**`. Added `@OpenAPIDefinition` at the infrastructure configuration level.
*   **Rationale:** Guarantees that the API contract is deterministically generated during the AST compilation phase and served as static assets by Netty, ensuring zero CPU overhead during production traffic handling.

---

## 3. Consequences

### Positive
*   **Absolute Pipeline Stability:** The Gradle build executes seamlessly across all environments without legacy deprecation warnings.
*   **Fault-Tolerant Pipeline:** The Netty HTTP event loop is now immune to tracing SDK classpath failures.
*   **Military-Grade Security:** The container image attack surface is reduced to the absolute minimum theoretical limit. Container escapes are mitigated at the kernel/orchestrator level.
*   **Compliance & Observability:** The API is fully documented and queryable, satisfying strict governance and discovery requirements.

### Negative
*   **Debugging Constraints:** Because the production container is Distroless, DevOps and SRE engineers cannot use `kubectl exec -it <pod> -- /bin/sh` to troubleshoot the live environment. All diagnostics *must* be performed via externalized telemetry (OpenTelemetry logs, metrics, and traces).
*   **Strict Probes Requirement:** Standard Kubernetes liveness probes using `curl` or `cat` are impossible; native HTTP probes must be used (which has already been implemented).