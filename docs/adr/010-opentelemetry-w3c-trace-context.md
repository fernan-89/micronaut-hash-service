# ADR-010: Adoption of OpenTelemetry and W3C Trace Context for Distributed Observability

**Status:** Accepted
**Date:** 2026-08-15
**Component:** Infrastructure / Observability
**Related Decisions:** ADR-008 (Reactive MDC Bridge), ADR-009 (Forensic Origin Telemetry)

## 1. Context and Problem Statement

Initially, our distributed tracing relied on a proprietary HTTP header (`X-Trace-Id`) and manually generated UUIDs injected into the SLF4J MDC. While this approach provided local context within the microservice, it created an "observability silo". In a global-scale architecture, requests traverse API Gateways, Service Meshes, and multiple downstream microservices. Relying on custom headers prevents unified, end-to-end visualization in standard APM (Application Performance Monitoring) tools like Datadog, Jaeger, or Grafana Tempo.

## 2. Decision

We will deprecate the custom `X-Trace-Id` mechanism and adopt **OpenTelemetry (OTEL)** as the definitive standard for distributed tracing, utilizing the official **W3C Trace Context** (`traceparent` and `tracestate` headers).

*   **Native OTEL Instrumentation:** We integrate `micronaut-tracing-opentelemetry-http` to automatically intercept incoming Netty HTTP requests and outbound reactive MongoDB drivers, generating compliant `Span` and `Trace` identifiers.
*   **W3C Compliance:** The application will strictly respect the `traceparent` header provided by upstream API Gateways. If absent, the OTEL SDK will seamlessly generate a root trace.
*   **Filter Adaptation:** The custom `TraceIdFilter` (ADR-009) is refactored to extract the trace ID directly from the active OTEL `SpanContext` (`Span.current().getSpanContext().getTraceId()`) rather than parsing proprietary headers. This guarantees that forensic logs (IP, User-Agent) are perfectly correlated with the globally distributed OTEL spans.

## 3. Consequences

### Positive
*   **Global Ecosystem Compatibility:** The microservice is now fully compatible with modern Service Meshes (Istio/Linkerd) and major observability platforms without vendor lock-in.
*   **Span-Level Granularity:** Beyond logs, engineering teams can now measure exact latency boundaries (e.g., HTTP execution vs. MongoDB query execution) through span flame-graphs.
*   **Code Simplification:** The framework handles trace ID generation and header propagation, offloading this responsibility from custom application code.

### Negative
*   **Dependency Footprint:** Introduces additional OTEL SDK dependencies, marginally increasing the binary size and JVM startup time.
*   **Postman/Testing Adjustments:** Test suites and Postman collections must be updated to either rely on automatic span generation or inject standard `traceparent` headers rather than simple UUIDs.