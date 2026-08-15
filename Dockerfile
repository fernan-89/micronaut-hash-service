# ==============================================================================================
# /**
#  * @file        Dockerfile
#  * @module      Container Image Build Manifest (JIT Optimized)
#  * @description Multi-stage Docker build pipeline for Java/Micronaut microservices.
#  *              Optimized for minimal footprint, strict security (Zero-Trust distroless),
#  *              Ahead-of-Time (AOT) bytecode precomputation, and BuildKit layer caching.
#  *
#  * @maintainer  Thinklab Systems Engineering Team
#  * @target      Production (Mission-Critical NASA-Level Standard)
#  */
# ==============================================================================================

# ----------------------------------------------------------------------------------------------
# /**
#  * @section     Stage 1: Build & Compilation (AOT Enabled)
#  * @description Leverages the official Temurin JDK 21 image. Uses the project's native Gradle
#  *              Wrapper to ensure version immutability.
#  */
# ----------------------------------------------------------------------------------------------
FROM eclipse-temurin:21-jdk-jammy AS builder

# Set the working directory for the build process
WORKDIR /home/gradle/src

# 1. CACHE OPTIMIZATION: Copy Gradle wrapper and dependency manifests first.
COPY --chown=root:root gradlew ./
COPY --chown=root:root gradle/ ./gradle/
COPY --chown=root:root build.gradle settings.gradle* gradle.properties ./

# Make the wrapper executable
RUN chmod +x gradlew

# 2. DEPENDENCY LAYER: Download dependencies to cache them.
# This prevents re-downloading the internet on every code change.
RUN --mount=type=cache,target=/root/.gradle/caches \
    --mount=type=cache,target=/root/.gradle/wrapper \
    ./gradlew dependencies --no-daemon

# 3. SOURCE COMPILATION: Copy the actual source code.
COPY --chown=root:root src/ ./src/

# 4. EXECUTE BUILD: Compiles AST, runs AOT optimizations, and packages the Fat JAR.
# 'assemble' skips testing during the Docker build (tests should run in the CI pipeline).
RUN --mount=type=cache,target=/root/.gradle/caches \
    --mount=type=cache,target=/root/.gradle/wrapper \
    ./gradlew assemble -x test --no-daemon --parallel

# ----------------------------------------------------------------------------------------------
# /**
#  * @section     Stage 2: Runtime Environment (Zero-Trust)
#  * @description Utilizes Google's Distroless image. Contains ONLY the JVM and essential
#  *              C libraries. No shell (/bin/sh), no package manager, no root access.
#  *              Immune to classic container escapes and reverse shell injections.
#  */
# ----------------------------------------------------------------------------------------------
FROM gcr.io/distroless/java21-debian12:nonroot

LABEL org.opencontainers.image.source="https://github.com/thinklab/micronaut-hash-service"
LABEL org.opencontainers.image.vendor="Thinklab Systems Engineering"
LABEL org.opencontainers.image.title="Hash Service"
LABEL org.opencontainers.image.security.policy="Zero-Trust Distroless"

# ----------------------------------------------------------------------------------------------
# /**
#  * @subsection  Security Context (Non-Root Execution)
#  * @description The distroless:nonroot image pre-defines the 'nonroot' user (UID 65532).
#  *              Enforces execution without elevated privileges.
#  */
# ----------------------------------------------------------------------------------------------
USER nonroot:nonroot

# Set the application execution directory
WORKDIR /app

# ----------------------------------------------------------------------------------------------
# /**
#  * @subsection  Application Binary
#  * @description Copies the AOT-optimized Fat JAR from the builder stage.
#  * @security    Enforces strict read-only permissions (chmod 444) to prevent binary
#  *              tampering in the event of an RCE (Remote Code Execution) vulnerability.
#  */
# ----------------------------------------------------------------------------------------------
COPY --from=builder --chown=nonroot:nonroot --chmod=444 /home/gradle/src/build/libs/*-all.jar app.jar

# ----------------------------------------------------------------------------------------------
# /**
#  * @subsection  Environment & Telemetry Variables
#  * @description Tuned for Kubernetes lifecycle, reactive Netty, and consistent telemetry.
#  *
#  * @tuning      -XX:+ExitOnOutOfMemoryError: Kills JVM instantly on memory starvation,
#  *               allowing Kubernetes to fail-fast and restart the pod (Zero-Zombie state).
#  *              -Duser.timezone=UTC: Mandated for distributed log consistency.
#  *              -Djava.awt.headless=true: Saves memory by avoiding UI resource allocations.
#  */
# ----------------------------------------------------------------------------------------------
ENV JAVA_TOOL_OPTIONS="-Xmx256m -XX:+UseContainerSupport -XX:+ExitOnOutOfMemoryError -Djava.awt.headless=true -Duser.timezone=UTC"
ENV MONGODB_URI="mongodb://localhost:27017/default_db_local"
ENV MICRONAUT_SERVER_PORT=8080

# Expose the standard HTTP port for the service mesh/ingress
EXPOSE ${MICRONAUT_SERVER_PORT}

# ----------------------------------------------------------------------------------------------
# /**
#  * @subsection  Container Entrypoint
#  * @description Direct exec form (no shell). The JVM assumes PID 1.
#  *              Ensures SIGTERM signals from the orchestrator are natively caught for
#  *              graceful shutdown of Netty and Micronaut components.
#  */
# ----------------------------------------------------------------------------------------------
ENTRYPOINT ["java", "-jar", "app.jar"]