# ADR 003: Architectural Separation and DTO Pattern for Hash Forensic Audit Trail

## Status
Accepted

## Context
We introduced a new feature to retrieve the complete, immutable forensic audit trail of state mutations for a specific cryptographic hash token via a new endpoint (`GET /hashes/{id}/audit`). This audit trail captures the full lifecycle of a hash, tracking operations like `HASH_GENERATION`, `HASH_DEACTIVATION`, and `HASH_REACTIVATION`.

Initially, the database audit logs fetched via the reactive pipeline (`Flux<HashAudit>`) were exposed directly through the `HashController` using the internal `HashAudit` domain entity.

Micronaut 4+ employs the `micronaut-serde` engine for reflection-free, high-performance Ahead-of-Time (AOT) JSON serialization. This engine requires serializable classes to be explicitly annotated with `@Serdeable` to generate introspection metadata at compile time. Exposing the domain model directly caused two critical architectural and runtime issues:
1. **Coupling Violation:** The core domain layer was forced to absorb infrastructure and web-specific annotations (`@Serdeable`, `@Schema`), violating the dependency rules of Clean Architecture.
2. **Runtime Failures (`CodecException`):** Because the domain model lacked compile-time serialization metadata, the Netty server failed to encode the reactive stream data accumulated by `.collectList()`, resulting in `500 Internal Server Error` responses.

## Decision
We decided to implement the **Projection Pattern / Data Transfer Object (DTO)** specifically to support the new Hash Audit Trail feature, isolating the web exposure from the domain core.

Implementation specifications:
1. **Feature Delivery:** A new `HashAuditResponse` Java Record was created under `infrastructure.adapter.in.web.dto.response` to project and sanitize forensic data (such as mapping string values, handling execution timestamps, and securing contextual metadata maps) into a public API payload.
2. **Domain Isolation:** The core business entity `HashAudit` remains completely agnostic of web formatting, holding no framework-specific serialization or OpenAPI annotations.
3. **Reactive Transformation:** The `HashController` applies the transformation explicitly within the reactive stream using `.map(HashAuditResponse::fromDomain)` on the `Flux` returned by the use case, ensuring only `@Serdeable` compliant objects are aggregated into the final `Mono<HttpResponse<List<HashAuditResponse>>>`.

## Consequences

### Positive:
* **Successful Feature Delivery:** The forensic audit trail is now safely and correctly exposed over HTTP without triggering encoding errors.
* **Architectural Cleanliness:** Ensures strict compliance with Hexagonal/Clean Architecture principles by keeping the core domain pure.
* **Optimized Performance:** Micronaut Serde processes the `@Serdeable` response records natively without relying on runtime reflection, making the endpoint GraalVM-ready.

### Negative:
* **Maintenance Overhead:** Introducing the audit trail feature required additional boilerplate code to map the domain model data to its respective web presentation layer.