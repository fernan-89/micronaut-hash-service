# ADR-006: Proactive MongoDB Connection Warm-up and Resilient Circuit Breaking

**Status:** Accepted
**Date:** 2026-08-10
**Component:** Infrastructure / Health & Telemetry
**Related Decisions:** ADR-001 (Strict Constructor Injection), ADR-002 (Resilience & Circuit Breaking)

## 1. Context and Problem Statement

In containerized environments orchestrated by Kubernetes, applications often experience "cold starts" regarding database connectivity. The MongoDB Reactive Streams Driver relies on the Server Discovery and Monitoring (SDAM) specification, which initializes topologies asynchronously.

When a pod starts, Kubernetes immediately begins polling the `/health` readiness probe. If the SDAM topology is not yet resolved, the driver may report an `UNKNOWN` state or timeout, leading to transient probe failures. Furthermore, accepting HTTP traffic before the connection pool is fully initialized can result in connection spikes, latency penalties for the first user requests, or cascading failures if the database is temporarily unreachable or misconfigured.

Additionally, duplicating the database name in configurations (e.g., within the connection URI and as a separate property) leads to configuration drift and potential runtime mismatches.

## 2. Decision

We will implement a proactive, non-blocking warmup sequence tied to the Micronaut IoC `StartupEvent` (`MongoWarmupObserver`). This component acts as a **Two-Phase Verification System** and a **Passive Circuit Breaker**.

### 2.1. Dynamic Configuration Parsing
To eliminate configuration drift, the component will dynamically parse the standard `mongodb.uri` property using the driver's native `ConnectionString` class to extract the target application database.

### 2.2. Two-Phase Topology Warmup
The component will execute a deterministic BSON `ping` command using Project Reactor:
*   **Phase 1 (Cluster Health):** Ping the `admin` database to force SDAM to discover the cluster topology and warm up the connection pool.
*   **Phase 2 (Contextual Health):** Ping the dynamically resolved application database to verify Role-Based Access Control (RBAC) and logical database availability.

### 2.3. Passive Circuit Breaker and Progressive Backoff
Since the warmup process is asynchronous, it does not block the application's main thread (Micronaut will report "Server Running"). However, the native health indicator will report `DOWN` until the warmup succeeds, effectively acting as a **Passive Circuit Breaker** that prevents Kubernetes from routing HTTP traffic to the pod.

If the database is unreachable, a progressive retry policy will be applied asynchronously:
*   **Attempt 1:** 15 seconds backoff.
*   **Attempt 2:** 30 seconds backoff.
*   **Attempt 3:** 60 seconds backoff.

### 2.4. Graceful Degradation and Container Restart
If all 3 retry attempts are exhausted (totaling over 105 seconds of unreadiness), the database is deemed definitively unreachable. The component will invoke `ApplicationContext.stop()`, gracefully shutting down the JVM. This delegates the healing process back to the orchestration layer (Kubernetes), which will kill the container and schedule a fresh pod.

## 3. Consequences

### Positive
*   **Zero Cold-Start Latency:** The first HTTP request routed to the pod will hit a fully initialized and warmed-up MongoDB connection pool.
*   **Self-Healing:** Network partitions or database restarts during pod initialization are handled gracefully via progressive backoff without human intervention.
*   **DRY Configuration:** Dynamically parsing the MongoDB URI guarantees that the health check and the application always target the exact same database, reducing misconfiguration risks.
*   **Orchestrator Synergy:** Failing the readiness probe (passively) while retrying prevents dropped requests. Shutting down the container on terminal failure aligns with standard Kubernetes fail-fast principles.

### Negative
*   **Increased Complexity:** Introduces complex Reactive Streams error handling (e.g., explicit `.subscribe()` consumers to prevent `onErrorDropped` exceptions).
*   **Delayed Readiness:** Pods will take longer to report as `READY` in Kubernetes, which must be accounted for in the deployment strategy (e.g., configuring `initialDelaySeconds` or `timeoutSeconds` properly in the deployment manifests).