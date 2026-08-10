# ADR-008: Reactive MDC Context Propagation Bridge for Distributed Tracing

**Status:** Accepted
**Date:** 2026-08-10
**Component:** Infrastructure / Telemetry & Observability
**Related Decisions:** ADR-003 (Non-Blocking Observability & Telemetry), ADR-007 (Synchronous Barriers)

## 1. Context and Problem Statement

Standard logging frameworks like SLF4J rely on `ThreadLocal` storage for Mapped Diagnostic Context (MDC) attributes such as correlation identifiers (`traceId`). In traditional thread-per-request servlet models, the thread remains constant throughout the request lifecycle.

However, in ultra-high-throughput reactive architectures (Micronaut + Netty + Project Reactor), execution constantly hops across different asynchronous thread boundaries—transitioning from Netty event loops (`ntLoopGroup`) to background worker thread pools (`andler-executor`) during database queries or business orchestration. Because `ThreadLocal` cannot natively follow reactive streams across these thread jumps, the `traceId` context gets dropped mid-stream, resulting in incomplete logs marked as `[trace=NONE]` during critical execution and error-handling phases.

## 2. Decision

We will implement a global **Project Reactor to SLF4J MDC Bridge** (`ReactorMdcBridge`) combined with Project Reactor's automatic context propagation hooks (`Hooks.enableAutomaticContextPropagation()`).

*   **Operator Interception:** We register a global operator modifier (`Operators.lift`) that intercepts every reactive stream operator lifecycle stage.
*   **Context-to-Thread Synchronization:** A custom `CoreSubscriber` wrapper (`MdcCoreSubscriber`) inspects the active Reactor `Context` (`Context.currentContext()`) on every signal emission (`onNext`, `onError`, `onComplete`), automatically pulling the `traceId` and binding it to the active physical thread's SLF4J `MDC`.
*   **Context Isolation & Cleanup:** If a stream lacks a tracking context or terminates, the MDC key is safely purged (`MDC.remove`), preventing data pollution and context leaks across pooled threads.

## 3. Consequences

### Positive
*   **End-to-End Observability:** Complete architectural traceability from the edge HTTP boundary (Netty) down to data persistence layers and global exception handlers, ensuring 100% trace correlation coverage.
*   **Forensic Reliability:** Simplifies root-cause analysis and log aggregation in centralized systems (ELK, Datadog) by guaranteeing uniform `traceId` stamping across all asynchronous jumps.
*   **Zero Architectural Friction:** Developers do not need to manually thread context parameters through use cases or reactive chains.

### Negative
*   **Slight Overhead:** Intercepting every reactive operator introduces a marginal CPU overhead, which is heavily offset by the immense value of uniform distributed logging in high-scale systems.