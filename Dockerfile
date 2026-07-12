# ==============================================================================
# Thinklab Hash Service - Multi-Stage Docker Build
# ------------------------------------------------------------------------------
# This Dockerfile utilizes a multi-stage approach to ensure a clean, secure,
# and lightweight production image.
# ==============================================================================

# ------------------------------------------------------------------------------
# Stage 1: Build & Compilation
# ------------------------------------------------------------------------------
# Uses the official Gradle image with JDK 21 to compile the source code and
# generate an executable Fat JAR.
FROM gradle:8-jdk21-jammy AS builder

# Set the working directory for the build process
WORKDIR /home/gradle/src

# Copy project files while maintaining proper ownership for the gradle user
COPY --chown=gradle:gradle . .

# Execute the shadowJar task to bundle the application and all its dependencies
# into a single, portable JAR file.
RUN gradle shadowJar --no-daemon

# ------------------------------------------------------------------------------
# Stage 2: Runtime Environment
# ------------------------------------------------------------------------------
# Uses a minimal Eclipse Temurin JRE image for production deployment to reduce
# the attack surface and image size.
FROM eclipse-temurin:21-jre-jammy

# Create a dedicated group and user to run the application with minimal privileges,
# enhancing container security.
RUN groupadd -r thinklab && useradd -r -g thinklab hash-service
USER hash-service

# Set the application directory
WORKDIR /app

# Copy only the bundled 'all' JAR from the builder stage, effectively discarding
# the source code and build tools to keep the image slim.
COPY --from=builder /home/gradle/src/build/libs/*-all.jar app.jar

# Define default memory limits and ensure Java respects container orchestration
# memory and CPU constraints.
ENV JAVA_OPTS="-Xmx512m -XX:+UseContainerSupport"

# Expose the standard Micronaut application port
EXPOSE 8080

# Use a shell wrapper for the entrypoint to allow for environment variable
# expansion (e.g., JAVA_OPTS) during container startup.
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]