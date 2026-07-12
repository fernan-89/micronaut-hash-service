# ADR 002: Partial Updates for State Transitions in Reactive Repositories

## Status
Accepted

## Context
During execution of state transitions on existing domain aggregates (`HashToken`) across multiple Use Cases (`Deactivate`, `Reactivate`, `Revoke`), the reactive pipeline threw `MongoWriteException` with `code=11000 (duplicate key error)`. 

Investigation revealed that Micronaut Data’s default `save()` method interprets instances with missing or unmapped framework-specific metadata (such as `@Version` or state tracking flags) as completely new documents. Because our Domain Layer remains 100% pure, decoupled, and stateless—free of database lifecycle fields—mapping a domain object back to an infrastructure entity reset these tracking properties, causing the repository to erroneously execute an `INSERT` statement instead of an `UPDATE`, leading to primary ID collisions on the `_id` field.

## Decision
We have decided to replace monolithic entity replacements with **Explicit Partial Updates (`$set` operations)** driven by custom query methods within our repository interfaces, separating creation logic from state mutations.

### Key Architectural Guidelines:
1. **Command vs. Query Separation at Repository Level:** 
   * `save()` is strictly reserved for the creation of new documents (e.g., `GenerateHashInteractor`, `HashAuditRepositoryPort`).
   * `update()` is mandatory for changing the state of pre-existing aggregates.
2. **Annotation-Driven ID Updates:** Repositories will use `@Id` parameter markers to instruct Micronaut Data to execute explicit database updates targeting only the required state properties, bypassing entity dirty checking and optimistic locking version requirements.
3. **Domain Purity Retention:** The application domain models will remain completely agnostic of persistence mechanics. No version fields or database identifiers will be leaked into the domain to satisfy framework-level lifecycle states.

## Consequences

### Positive:
- **State Machine Integrity:** State transitions can now be reliably triggered without colliding with existing unique resource identifiers (`UUID`).
- **Performance Optimization:** Infrastructure adapters now stream ultra-focused `$set` queries directly to MongoDB, eliminating the overhead of copying and rewriting entire BSON documents.
- **Domain Decoupling:** Maintains the strict standard established in ADR 001; infrastructure framework quirks are isolated within the adapter layer without polluting domain records.
- **Concurrency Protection:** Partial updates on specific data points reduce the surface area for write conflicts across asynchronous parallel operations.

### Negative:
- **Maintenance Overhead:** Updates targeting specific field structures require explicit repository method definitions instead of relying on out-of-the-box global CRUD extensions.
- **Mapping Friction:** Adapters must manually extract individual properties from domain aggregates to pass them as distinct arguments to the native persistence method.

## Implementation Details
- **Repository Definition:** Interface query methods use localized parameter updates bound to identifiers:

    `Mono<Long> update(@Id UUID id, HashStatus status, String executor, String reason, Instant updatedAt);`

* **Pipeline Segregation:** Interactors handling updates must use `.flatMap(repositoryPort::update)` instead of `.save()`.
* **Asynchronous Execution:** Heavy calculation or cryptography tasks before state saves must continue to utilize decoupled schedulers (`Schedulers.parallel()`) to keep the Netty event loop non-blocking.

## Compliance

Every interactor mapping mutations on pre-existing records must undergo automated verification via reactive step unit tests to ensure no duplicate keys or `INSERT` paths are accidentally triggered on pre-allocated IDs.
