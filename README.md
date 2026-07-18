# Thinklab Hash Service

## Overview
A mission-critical microservice responsible for managing the complete lifecycle of cryptographic hash tokens. Engineered with Hexagonal Architecture (Ports and Adapters) and a fully Reactive Stack to guarantee high throughput, zero-blocking I/O, and structural maintainability.

## Tech Stack
* **Language & Runtime:** Java 21 / Micronaut Framework 4.4.2
* **Reactive Core:** Project Reactor (Mono / Flux)
* **Persistence:** MongoDB (Reactive Driver via Micronaut Data MongoDB)
* **Testing Suite:** JUnit 5, Mockito, Testcontainers
* **API Documentation:** OpenAPI / Swagger (Generated at compile-time)

## Architecture Model
```text
src/main/java/com/thinklab/
├── domain/                  # Core Business Logic (Rich Models, Value Objects, Pure Exceptions)
├── application/             # Orchestration & Use Cases (Inbound/Outbound Ports, Interactors)
└── infrastructure/          # External Integrations (REST Controllers, MongoDB Adapters, Configs)

```

## Core Features

* **Hexagonal Architecture:** Strict boundary enforcement ensuring the Core Domain is pure and framework-agnostic.
* **Forensic Audit Trail:** Complete immutable tracking of all state mutations (`GENERATION`, `DEACTIVATION`, `REACTIVATION`, `REVOCATION`).
* **Defensive Engineering:** Strict nullability controls (`@Nonnull`/`@Nullable`), compile-time Jakarta validations, and immutable DTOs.
* **Resilient Exception Boundaries:** Structural decoupling of Business Exceptions from Infrastructure Failures, ensuring correct HTTP status mapping.
* **Observability-Ready:** Standardized logging patterns, structured error responses, and integrated Liveness/Readiness probes.

## Operations

### Build & Run

```bash
# Clean and Build
./gradlew clean build --refresh-dependencies

# Start the Service
./gradlew run

```

### Running Tests

Test suite includes Unit Tests (Mockito) and Integration Tests (Testcontainers).

```bash
./gradlew test

```

### Infrastructure & API Endpoints

* **Health Status:** `http://localhost:8080/health`
* **Swagger UI:** `http://localhost:8080/swagger/views/swagger-ui`
* **OpenAPI YAML:** `http://localhost:8080/swagger/thinklab-hash-service-1.0.0.yml`

### E2E Testing (Postman)

A complete Postman collection is available covering the full lifecycle. It includes automated test scripts (`pm.test`) to validate structural integrity and business rules.

```

```