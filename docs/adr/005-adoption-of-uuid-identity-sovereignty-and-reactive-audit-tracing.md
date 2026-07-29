# ADR 005: Adoption of UUID Identity Sovereignty and Reactive Audit Tracing

## Status
Accepted

## Context
As part of our high-assurance, mission-critical cryptographic token registry, ensuring strict type safety, zero-copy serialization efficiency, and uncompromised forensic accountability is paramount. Previous iterations utilized string-based identifiers and mixed validation paradigms across application boundaries. To align with modern MongoDB BSON Binary (Subtype 4) performance standards and guarantee robust Zero Trust compliance, we needed to formalize architectural decisions regarding identifier sovereignty, immutable command encapsulation, proactive telemetry, and asynchronous logging.

## Decision
We have decided to enforce **Native UUID Identity Sovereignty**, **Compact Record-Based Defensive Commands/Queries**, **Proactive Infrastructure Warmup & Health Telemetry**, and **Asynchronous Non-Blocking I/O Logging** across the entire application stack.

### Key Components:
1.  **Identity Sovereignty (UUID Subtype 4):** All domain aggregates, queries, commands, and repository ports transition from string-based IDs to native Java `UUID` parameters, ensuring optimized BSON binary indexing and preventing cross-type contamination.
2.  **Defensive Application Commands & Queries:** Implemented as immutable Java records with explicit compact constructors executing fail-fast validation checks and normalizations.
3.  **Proactive Infrastructure Warmup:** Integration of startup event observers (`MongoWarmupObserver`, `ExternalEndpointsHealthIndicator`) to eagerly initialize connection pools, resolve DNS, and execute SDAM topology discovery prior to Kubernetes traffic ingestion.
4.  **Asynchronous High-Throughput Logging:** Configuration of Logback with non-blocking `AsyncAppenders` and specialized isolated audit paths (`ASYNC_AUDIT`) to prevent thread starvation on Netty reactive event loops.

## Consequences

### Positive:
- **Performance & Storage Efficiency:** Native `UUID` usage optimizes database index performance via BSON Binary Subtype 4 compatibility.
- **Fail-Fast Reliability:** Compact constructors with strict null-safety and validation intercept malformed payloads at the application edge before touching domain logic.
- **Observability & Zero Cold-Start:** Proactive dependency warming eliminates initial probe timeouts and ensures Kubernetes readiness health accuracy.
- **Non-Blocking Telemetry:** Asynchronous log appenders safeguard reactive event loops from disk I/O bottlenecks.

### Negative:
- **Migration Overhead:** Requires strict discipline across all layers (controllers, use cases, ports, and tests) to adhere to `UUID` typing instead of raw strings.
- **Boilerplate Constraints:** Demands rigorous adherence to validation rules and explicit dependency injection standards.

## Implementation Details
- **Identifiers:** Use `java.util.UUID` across all commands (`DeactivateHashCommand`, `RevokeHashCommand`, etc.) and repository ports.
- **Validation:** Enforce Jakarta Bean Validation (`@NotNull`, `@NotBlank`) coupled with runtime assertion guards in record compact constructors.
- **Telemetry Probes:** Custom health indicators and warmup observers bound to strict timeout thresholds to prevent thread starvation.
- **Logging Topology:** Logback XML configured with zero discarding thresholds for critical audit trails (`ASYNC_AUDIT`).

## Compliance
All new code and infrastructure modifications must strictly follow the `com.thinklab` package structure, maintain native UUID boundaries, and preserve non-blocking reactive stream paradigms.