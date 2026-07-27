# ==============================================================================================
# /**
#  * @file        Dockerfile
#  * @module      Container Image Build Manifest
#  * @description Multi-stage Docker build pipeline for Java/Micronaut microservices.
#  *              Optimized for minimal footprint, strict security (non-root execution),
#  *              and deterministic container-native JVM resource allocation.
#  *
#  * @maintainer  Platform Engineering Team
#  */
# ==============================================================================================

# ----------------------------------------------------------------------------------------------
# /**
#  * @section     Stage 1: Build & Compilation
#  * @description Leverages the official Gradle image with JDK 21 to compile source code
#  *              and package the application into an executable Fat JAR (shadowJar).
#  */
# ----------------------------------------------------------------------------------------------
FROM gradle:8-jdk21-jammy AS builder

# Set the working directory for the build process
WORKDIR /home/gradle/src

# Copy project files while maintaining proper ownership for the gradle user
COPY --chown=gradle:gradle . .

# Execute the shadowJar task to bundle the application and all dependencies
# into a single, highly portable JAR file.
RUN gradle shadowJar --no-daemon

# ----------------------------------------------------------------------------------------------
# /**
#  * @section     Stage 2: Runtime Environment
#  * @description Utilizes a minimal Eclipse Temurin JRE image to reduce attack surface
#  *              and image size. Discards all build tools and source code.
#  */
# ----------------------------------------------------------------------------------------------
FROM eclipse-temurin:21-jre-jammy

# ----------------------------------------------------------------------------------------------
# /**
#  * @subsection  Security Context (Non-Root Execution)
#  * @description Mitigates privilege escalation attacks by enforcing a strictly unprivileged
#  *              runtime user. NEVER run containers as root in production.
#  */
# ----------------------------------------------------------------------------------------------
RUN groupadd -r platform && useradd -r -g platform appuser
USER appuser

# Set the application execution directory
WORKDIR /app

# Copy only the bundled 'all' JAR from the builder stage
COPY --from=builder /home/gradle/src/build/libs/*-all.jar app.jar

# ----------------------------------------------------------------------------------------------
# /**
#  * @subsection  Environment & Telemetry Variables
#  * @description Default fallbacks for local execution. In production, these are injected
#  *              dynamically via Kubernetes ConfigMaps and Secrets.
#  *              - XX:+UseContainerSupport: Ensures JVM respects cgroup limits (RAM/CPU).
#  */
# ----------------------------------------------------------------------------------------------
ENV JAVA_OPTS="-Xmx256m -XX:+UseContainerSupport"
ENV MONGODB_URI="mongodb://localhost:27017/default_db_local"
ENV PORT=8080

# Expose the standard HTTP port for the service mesh/ingress
EXPOSE ${PORT}

# ----------------------------------------------------------------------------------------------
# /**
#  * @subsection  Container Entrypoint
#  * @description Uses a shell wrapper (sh -c) to guarantee environment variable expansion
#  *              (e.g., JAVA_OPTS, PORT, MONGODB_URI) during JVM bootstrapping.
#  */
# ----------------------------------------------------------------------------------------------
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]