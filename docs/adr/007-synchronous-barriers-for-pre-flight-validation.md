# ADR-007: Synchronous Barriers for Pre-Flight Validation

**Status:** Accepted
**Date:** 2026-08-10
**Component:** Infrastructure / Application Lifecycle
**Related Decisions:** ADR-002 (Resilience & Circuit Breaking), ADR-006 (Proactive MongoDB Warm-up)

## 1. Context and Problem Statement

Reactive frameworks like Micronaut, paired with Project Reactor, are designed to execute operations asynchronously. During the application initialization phase (`StartupEvent`), dispatching network requests (such as MongoDB SDAM pings or HTTP external endpoint checks) using non-blocking subscribers (e.g., `.subscribe()`) releases the main thread immediately.

Consequently, the framework considers the startup phase complete and binds the HTTP server to its port, logging `Server Running`. However, the background tasks verifying critical dependencies are still in flight. If an orchestration platform (like Kubernetes) routes traffic to the pod during this window, incoming HTTP requests will fail, causing transient errors for users. The application must guarantee that it is fully operational *before* accepting external traffic.

## 2. Decision

We will enforce a **Synchronous Barrier** design pattern during the application's startup phase.

Instead of asynchronously subscribing to the warm-up publishers, we will intentionally block the main initialization thread using Reactor's blocking operators (`.block()` for `Mono` and `.blockLast()` for `Flux`).

*   **Bounded Waits:** Every synchronous block must be bounded by a strict timeout (e.g., `Duration.ofSeconds(120)`) to prevent indefinite hanging if a network socket silently drops.
*   **Fail-Fast before Port Binding:** If a critical dependency is definitively unreachable (exhausting all circuit-breaker retries), the application context will be shut down *before* the HTTP server is started.
*   **Lifecycle Isolation:** Blocking operations are strictly forbidden during runtime (event-loop phase) but are explicitly authorized and required during the initialization phase (`StartupEvent`).

## 3. Consequences

### Positive
*   **Strict Traffic Routing:** Kubernetes Readiness Probes will never hit a partially initialized application. Traffic is only routed to the pod when 100% of the internal state and external dependencies are validated.
*   **True Fail-Fast:** If misconfigurations (e.g., wrong database credentials) or network partitions exist, the pod crashes cleanly before ever listening on port 8080.
*   **Deterministic Logs:** Startup logs become strictly sequential and deterministic, making infrastructure debugging significantly easier.

### Negative
*   **Increased Startup Time:** The total time to boot the pod increases proportionally to network latency and active circuit-breaker retry delays. Orchestration manifests must account for this by adjusting `initialDelaySeconds` or `timeoutSeconds` for probes.
*   **Reactive Anti-Pattern Exception:** Developers must understand the context boundary; blocking is used here as a deliberate infrastructure mechanism, not as a standard application-level practice.