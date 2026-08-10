# ADR-009: Forensic Origin Telemetry and Privacy-Preserving Logging

**Status:** Accepted
**Date:** 2026-08-15
**Component:** Infrastructure / Telemetry & Observability
**Related Decisions:** ADR-003 (Non-Blocking Observability & Telemetry), ADR-008 (Reactive MDC Context Propagation Bridge)

## 1. Context and Problem Statement

In a highly secure cryptographic hash registry, tracing the internal lifecycle of a request (`traceId`) is insufficient for a complete forensic audit. To detect anomalies, prevent abuse, and accelerate troubleshooting, the system must also identify the *origin* of the request (Client IP and User-Agent).

However, logging origin metadata introduces significant operational and legal risks:
1.  **Regulatory Compliance (LGPD/GDPR):** A raw IP address is classified as Personally Identifiable Information (PII). Persisting raw IPs in application logs or centralized observability platforms (e.g., ELK, Datadog) triggers strict data retention, anonymization, and "right to be forgotten" obligations.
2.  **Log Bloat & Injection Vectors:** Malicious actors can spoof HTTP headers, sending massive `User-Agent` strings designed to exhaust disk space, inflate SIEM costs, or trigger Out-Of-Memory (OOM) errors during string allocation.

## 2. Decision

We will implement a **Privacy-Preserving Forensic Telemetry Matrix** at the edge of our reactive pipeline, extending the existing distributed tracing infrastructure.

*   **Extraction & Truncation:** The `TraceIdFilter` will extract the `User-Agent` header and strictly truncate it to 40 characters. This preserves enough entropy for browser/client identification while neutralizing log-bloating attack vectors.
*   **Privacy by Design (IP Obfuscation):** The filter will intercept the `X-Forwarded-For` header (or raw Netty remote address) and dynamically mask the final network segment (e.g., `192.168.1.150` becomes `192.168.1.***`, and IPv6 blocks are similarly masked). This definitively strips the PII classification while retaining enough subnet data for broad geographic and topological troubleshooting.
*   **Matrix Propagation:** The `ReactorMdcBridge` (ADR-008) is expanded to support a dynamic array of MDC keys (`traceId`, `clientIp`, `userAgent`), ensuring this origin data flawlessly survives asynchronous thread-hopping across the Project Reactor boundaries.
*   **Global Exception Availability:** These attributes are simultaneously bound to the `HttpRequest` attributes, ensuring the `GlobalExceptionHandler` can retrieve and log the exact origin of a request even if the reactive stream catastrophically fails.

## 3. Consequences

### Positive
*   **Regulatory Immunity:** By applying structural obfuscation at the ingress boundary, the microservice's logs are instantly stripped of PII, ensuring default compliance with LGPD/GDPR without relying on external log-scrubbing tools.
*   **Enhanced Forensic Observability:** Security and engineering teams immediately gain visibility into the originating client types and subnets for every business rule violation or infrastructure fault.
*   **Cost & Resource Protection:** Truncating unvalidated headers protects the JVM heap and reduces indexing costs in downstream SIEM/Log Aggregation tools.

### Negative
*   **Granular IP Loss:** The microservice cannot independently perform exact IP-based rate limiting or blocking using its internal logs. *(Mitigation: This is an accepted architectural trade-off, as exact IP access control and raw Access Logs are the strict responsibility of the API Gateway and Web Application Firewall [WAF], not the internal business microservice).*