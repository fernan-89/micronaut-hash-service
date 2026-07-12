# ADR 001: Adoption of Hexagonal Architecture with Reactive Stack

## Status
Accepted

## Context
Our project requires a robust, testable, and scalable architecture capable of handling high-throughput asynchronous operations. The current monolithic or tightly coupled approaches pose risks to long-term maintenance and unit testing efficiency. We need a clear separation between business logic and infrastructure concerns to ensure "NASA-level" reliability.

## Decision
We have decided to implement **Hexagonal Architecture (Ports and Adapters)** combined with a **Reactive Stack** (Micronaut, Project Reactor/Netty, and MongoDB Reactive Streams).

### Key Components:
1.  **Domain Layer:** Pure Java 21 Records and Enums representing business rules, isolated from any framework dependency.
2.  **Application Layer:** Use Cases (Interactors) acting as Input Ports to orchestrate business flows.
3.  **Infrastructure Layer:** Adapters (Repositories, Controllers, Clients) implementing Ports to communicate with external systems (MongoDB, HTTP, etc.).

## Consequences

### Positive:
- **Testability:** Business logic is now 100% testable using JUnit 5 and Mockito without needing a running database or HTTP container.
- **Flexibility:** We can swap external infrastructure adapters (e.g., changing from MongoDB to a different database) with minimal impact on domain logic.
- **Resilience:** The reactive stack ensures non-blocking I/O, optimizing resource usage under high load.
- **Clarity:** Strict separation of concerns prevents "code rot" and ensures that developers know exactly where to implement new features.

### Negative:
- **Complexity:** Increased initial boilerplate due to the creation of interfaces, DTOs, and mapping layers.
- **Learning Curve:** Requires discipline from the team to maintain strict boundary segregation.

## Implementation Details
- **Java 21:** Use `records` for immutable data structures.
- **Validation:** Use Jakarta Bean Validation in the Input Ports (Commands) to ensure data integrity at the boundary.
- **Error Handling:** Granular hierarchy separating Technical Exceptions (infrastructure) from Business Exceptions (logic).
- **Documentation:** Every public interface must be documented with Javadoc, and all structural changes must be recorded via ADRs.

## Compliance
All new code must adhere to the `com.thinklab` package structure defined in the `as-is` architecture audit.