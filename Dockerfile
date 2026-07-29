# ==============================================================================================
# /**
#  * @file        Dockerfile
#  * @module      Container Image Build Manifest
#  * @description Multi-stage Docker build pipeline for Java/Micronaut microservices.
#  *              Optimized for minimal footprint, strict security (Zero-Trust distroless),
#  *              and optimal layer caching.
#  *
#  * @maintainer  Platform Engineering Team
#  */
# ==============================================================================================

# ----------------------------------------------------------------------------------------------
# /**
#  * @section     Stage 1: Build & Compilation
#  * @description Leverages the official Gradle image with JDK 21 to compile source code.
#  *              Implements aggressive layer caching for dependency resolution.
#  */
# ----------------------------------------------------------------------------------------------
FROM gradle:8-jdk21-jammy AS builder

# Set the working directory for the build process
WORKDIR /home/gradle/src

# 1. CACHE OPTIMIZATION: Copy ONLY dependency manifests first.
# This layer is cached and only rebuilt if the build.gradle file changes.
COPY --chown=gradle:gradle build.gradle settings.gradle* gradle.properties ./

# Resolve and download dependencies (stored in Docker build cache)
# Note: The '|| true' allows it to fail gracefully if no source code is present yet
RUN gradle dependencies --no-daemon || true

# 2. SOURCE COMPILATION: Copy the actual source code.
# This layer rebuilds frequently during development, but skips dependency downloads.
COPY --chown=gradle:gradle src/ src/

# Execute the shadowJar task to bundle the application and all dependencies
RUN gradle shadowJar --no-daemon

# ----------------------------------------------------------------------------------------------
# /**
#  * @section     Stage 2: Runtime Environment (Zero-Trust)
#  * @description Utilizes Google's Distroless image. Contains ONLY the JVM and essential
#  *              C libraries. No shell (/bin/sh), no package manager, no root access.
#  */
# ----------------------------------------------------------------------------------------------
FROM gcr.io/distroless/java21-debian12:nonroot

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

# Copy only the bundled 'all' JAR from the builder stage
COPY --from=builder /home/gradle/src/build/libs/*-all.jar app.jar

# ----------------------------------------------------------------------------------------------
# /**
#  * @subsection  Environment & Telemetry Variables
#  * @description JAVA_TOOL_OPTIONS is automatically picked up by the JVM without needing a
#  *              shell wrapper. Static memory limits kept per engineering request.
#  */
# ----------------------------------------------------------------------------------------------
ENV JAVA_TOOL_OPTIONS="-Xmx256m -XX:+UseContainerSupport"
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