# ADR 012: Execution Context Forensics, SRE Telemetry & MDC Boot/Shutdown Propagation

**Date:** 2026-08-16  
**Status:** Accepted & Implemented  
**Authors:** Thinklab Systems Engineering Team  
**Tags:** Architecture, SRE, Observability, Telemetry, Forensics

---

## 1. Context & Problem Statement

As the Thinklab Hash Service achieved container readiness and multi-environment deployment capabilities (local bare-metal, Docker Compose, and Kubernetes meshes), the engineering team identified several critical observability and debugging friction points during application lifecycle events and ingress request handling:

1.  **Boot Telemetry Blind Spots (`[NONE]` Logs):** During the initial startup sequence (warmup observers, external health checks, and database SDAM topology discovery), asynchronous worker pools and reactive execution boundaries (e.g., `onPool-worker`, `async-driver`) wiped the ThreadLocal `Mapped Diagnostic Context` (MDC). This resulted in uninformative `[trace=NONE | ip=NONE | client=NONE]` markers in central logging systems.
2.  **Environment Ambiguity:** During production incidents, logs lacked a deterministic indicator defining *where* the binary was executing (whether on a developer's Windows workstation, an ephemeral Docker container, or a managed Kubernetes Pod), complicating root-cause analysis.
3.  **Shallow Ingress Caller Forensics:** Standard HTTP request logging only captured basic trace IDs, missing crucial ingress context such as the target virtual host (`Host` header), request provenance (`Origin`/`Referer`), and the corporate or public identity of the calling client.
4.  **Regulatory Compliance vs. Forensics (LGPD/GDPR):** Capturing raw client IP addresses for security auditing presented legal risks regarding Personal Identifiable Information (PII) tracking without a standardized privacy-masking protocol.

---

## 2. Architectural Decisions

To establish world-class Site Reliability Engineering (SRE) observability and eliminate forensic blind spots, the following architectural decisions were implemented:

### 2.1. Pre-Flight MDC Boot Context Injection & Thread Propagation
*   **Decision:** Programmatically inject a baseline `SYSTEM-BOOT` context into the SLF4J MDC at the very entry point (`Application.main()`) and explicitly assert it across reactive thread boundaries during startup observers.
*   **Implementation:** Developed explicit context-assertion routines (`injectBootContext()`) within `ExternalEndpointsHealthIndicator` and `MongoWarmupObserver`, ensuring that all initialization threads—including Netty workers, Reactor schedulers, and MongoDB driver pools—inherit the node's IP footprint and startup metadata.
*   **Rationale:** Eliminates all `[NONE]` log pollution, providing a continuous, uninterrupted audit trail from millisecond zero of application initialization.

### 2.2. Automated Execution Environment Probing (Infrastructure Forensics)
*   **Decision:** Natively scan filesystem signatures and orchestrator environment variables at startup to resolve the runtime topology.
*   **Implementation:** Created an environment scanner that inspects for container markers (e.g., `/.dockerenv`) and Kubernetes service variables (`KUBERNETES_SERVICE_HOST`), formatting a unified execution context descriptor (e.g., `Kubernetes Pod (Linux amd64)` or `Bare-Metal (Windows 10 amd64)`).
*   **Rationale:** Provides instant visibility into the execution tier directly within startup logs and actuator health endpoints (`/health`), accelerating distributed troubleshooting.

### 2.3. Edge Caller Forensics & Asynchronous Reverse DNS Lookup
*   **Decision:** Enrich ingress telemetry at the Netty gateway (`TraceIdFilter`) with multi-dimensional caller metadata.
*   **Implementation:**
    *   Extracted `Host` headers to record the target virtual host.
    *   Inspected `Origin` and `Referer` headers to map traffic provenance.
    *   Offloaded remote IP address resolution to a non-blocking, asynchronous Reverse DNS lookup (`InetAddress.getByName()`) bound to Reactor's `Schedulers.boundedElastic()`.
*   **Rationale:** Captures the external client's registered hostname without blocking the high-throughput Netty event loop threads, ensuring zero performance degradation under heavy traffic loads.

### 2.4. Privacy-by-Design IP Obfuscation (LGPD/GDPR Compliance)
*   **Decision:** Enforce mandatory PII masking on all ingress client IP addresses prior to logging.
*   **Implementation:** Implemented an obfuscation algorithm that truncates the final octet of IPv4 addresses (e.g., `192.168.1.150` -> `192.168.1.***`) and IPv6 blocks.
*   **Rationale:** Balances high-precision security auditing and forensic traceability with strict regulatory compliance regarding data protection.

---

## 3. Consequences

### Positive
*   **Zero-Blind-Spot Logging:** Application logs are 100% contextualized across every phase of execution (Boot, Warmup, Runtime, and Shutdown), completely eradicating `NONE` values.
*   **Accelerated Incident Response:** SRE and DevOps engineers can instantly identify the exact operating system, container tier, virtual host, and external client identity from any log line during an alert.
*   **Regulatory Compliance:** User privacy is legally protected through automated PII masking without sacrificing technical telemetry depth.
*   **Non-Blocking Resilience:** Expensive network I/O operations like Reverse DNS are safely isolated on elastic threads, preserving microservice throughput.

### Negative
*   **Log Footprint Expansion:** The inclusion of expanded metadata fields (`virtualHost`, `clientOrigin`, `externalClientHost`) slightly increases log volume and storage consumption in centralized log aggregators (e.g., Elasticsearch, Loki).
*   **DNS Resolution Latency Dependency:** Transient public DNS timeouts may occasionally cause minor delays in establishing the initial reverse hostname string for external callers, safely mitigated by fallback mechanisms to raw obfuscated IPs.