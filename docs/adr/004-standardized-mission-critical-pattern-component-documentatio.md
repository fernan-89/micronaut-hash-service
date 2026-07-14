# ADR 004: Standardized "Mission-Critical Pattern" for Component Documentation and Architecture

## Status
Accepted

## Context
As the Thinklab Hash Service evolved into a mission-critical infrastructure, we identified inconsistencies in documentation density, architectural clarity, and the strictness of layer boundaries. Previous implementations contained development-specific comments, inconsistent Javadoc formatting, and occasionally leaked infrastructure concerns into the Domain layer. Furthermore, the transition to high-performance reactive pipelines required a more robust standard for handling Data Transfer Objects (DTOs), Domain Entities, and global error handling to ensure GraalVM compatibility and compliance with RFC 7807.

## Decision
We have adopted the **"Mission-Critical Pattern"**, a set of strict architectural and documentation standards that govern the entire codebase. This pattern enforces consistency, maintainability, and architectural purity across all layers.

### 1. Documentation Standard (The "NASA" Pattern)
All components now follow a standardized, clean, and formal documentation style:
* **Language:** Professional English.
* **Format:** Mandatory inclusion of architectural role descriptions, clear Javadoc, and categorized architectural principles (e.g., *Projection Pattern*, *Anti-Corruption Layer*, *Immutability*).
* **Cleanup:** Total removal of internal development notes, "to-do" comments, or informal developer shorthand.
* **Focus:** Emphasis on the "Why" and "How" of the architectural role rather than implementation-level minutiae.

### 2. Architectural Layering and Decoupling
* **Domain Purity:** Domain models are strictly framework-agnostic. All persistence (`@MappedEntity`, `@Id`) and serialization (`@Serdeable`, `@Schema`) annotations were stripped from `com.thinklab.domain.*` and moved exclusively to their respective infrastructure adapters.
* **Projection Pattern:** All external-facing data is wrapped in explicit DTOs. Domain-to-Infrastructure translation is handled exclusively via static factory methods (`fromDomain()`) within the DTOs themselves, ensuring a clean contract between layers.
* **Global Resilience:** The `GlobalExceptionHandler` was standardized to transform all exceptions into RFC 7807 "Problem Details" payloads, ensuring a consistent contract for API consumers and masking internal technical failures.

### 3. Infrastructure & Performance
* **AOT & GraalVM Readiness:** All DTOs and entities are now optimized for Micronaut Serde, ensuring reflection-free serialization.
* **Reactive Integrity:** All infrastructure adapters and controllers are built to support non-blocking reactive streams (Project Reactor), with proper indexing strategies in MongoDB to support high-throughput forensic auditing.

## Consequences

### Positive:
* **Enhanced Maintainability:** Developers can now navigate any component and immediately understand its role and constraints through a unified documentation interface.
* **Decoupled Architecture:** Strict separation between Domain logic and Infrastructure (Web/DB) ensures the core business logic remains portable and testable.
* **Operational Stability:** Standardization of error handling and serialization metadata drastically reduces runtime failures (e.g., `CodecException` or `SerializationException`) in production.
* **GraalVM/AOT Readiness:** The removal of runtime reflection dependencies ensures the service remains performant and ready for native compilation.

### Negative:
* **Increased Initial Boilerplate:** Moving to explicit DTOs and mapping logic increases the number of classes, which requires more discipline during the implementation of new features.
* **Strict Discipline:** The rigorous standard for Javadoc and annotation placement requires developers to adhere to a specific mental model when contributing new code.