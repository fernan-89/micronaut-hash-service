# ==============================================================================================
# /**
#  * @file        Dockerfile
#  * @module      Container Image Build Manifest (GraalVM Native + UPX)
#  * @description Multi-stage Docker build pipeline for Java/Micronaut microservices.
#  *              Optimized for MAXIMUM performance, zero cold-start latency, and minimal footprint
#  *              using GraalVM Native Image, UPX compression, and Zero-Trust Distroless security.
#  *
#  * @maintainer  Thinklab Systems Engineering Team
#  * @target      Production (Mission-Critical Serverless & Edge Deployments)
#  */
# ==============================================================================================

# ----------------------------------------------------------------------------------------------
# /**
#  * @section     Stage 1: Build, Compilation & Compression
#  * @description Leverages the official GraalVM Community image for JDK 21.
#  */
# ----------------------------------------------------------------------------------------------
FROM ghcr.io/graalvm/native-image-community:21 AS builder

WORKDIR /home/gradle/src

# 0. SYSTEM DEPENDENCIES & UPX
# Install findutils (for Gradle wrapper), wget and xz (to download UPX)
RUN microdnf install -y findutils wget xz && microdnf clean all

# Download and install the UPX binary statically
RUN wget -q https://github.com/upx/upx/releases/download/v4.2.2/upx-4.2.2-amd64_linux.tar.xz \
    && tar -xf upx-4.2.2-amd64_linux.tar.xz \
    && mv upx-4.2.2-amd64_linux/upx /usr/bin/ \
    && rm -rf upx*

# 1. CACHE OPTIMIZATION
COPY --chown=root:root gradlew ./
COPY --chown=root:root gradle/ ./gradle/
COPY --chown=root:root build.gradle settings.gradle* gradle.propertie[s] ./

RUN chmod +x gradlew

# 1.5 DYNAMIC COMPILER CONFLICT RESOLUTION
# The Micronaut 4 plugin aggressively injects an outdated flag (-H:+SharedArenaSupport)
# that has been removed from GraalVM 21+, causing fatal build errors.
# We dynamically inject a Groovy instruction to strip this specific argument out
# during the build, fixing the conflict without modifying the host source code.
RUN echo "" >> build.gradle && \
    echo "graalvmNative { binaries { main { buildArgs.remove('-H:+SharedArenaSupport') } } }" >> build.gradle

# 2. DEPENDENCY LAYER
RUN --mount=type=cache,target=/root/.gradle/caches \
    --mount=type=cache,target=/root/.gradle/wrapper \
    ./gradlew dependencies --no-daemon

# 3. SOURCE COMPILATION
COPY --chown=root:root src/ ./src/

# 4. EXECUTE NATIVE BUILD
RUN --mount=type=cache,target=/root/.gradle/caches \
    --mount=type=cache,target=/root/.gradle/wrapper \
    ./gradlew nativeCompile -x test --no-daemon --parallel

# 5. ARTIFACT STANDARDIZATION & UPX COMPRESSION
# Find the generated binary, rename it, and compress it to reduce image size by ~50%.
RUN find build/native/nativeCompile/ -maxdepth 1 -type f -executable -exec mv {} /app-binary \; \
    && echo "--- [ ORIGINAL BINARY SIZE ] ---" \
    && ls -lh /app-binary \
    && echo "--- [ COMPRESSING WITH UPX ] ---" \
    && upx -7 /app-binary \
    && echo "--- [ COMPRESSED BINARY SIZE ] ---" \
    && ls -lh /app-binary

# ----------------------------------------------------------------------------------------------
# /**
#  * @section     Stage 2: Runtime Environment (Zero-Trust)
#  * @description Utilizes Google's Distroless 'base' image (glibc, NO JVM).
#  *              Contains ONLY essential OS libraries required to run native binaries.
#  */
# ----------------------------------------------------------------------------------------------
FROM gcr.io/distroless/base-debian12:nonroot

LABEL org.opencontainers.image.source="https://github.com/thinklab/micronaut-hash-service"
LABEL org.opencontainers.image.vendor="Thinklab Systems Engineering"
LABEL org.opencontainers.image.title="Hash Service (Native Compressed)"
LABEL org.opencontainers.image.security.policy="Zero-Trust Distroless Native"

USER nonroot:nonroot

WORKDIR /app

# Copy the compressed standalone native executable from the builder stage
COPY --from=builder --chown=nonroot:nonroot --chmod=555 /app-binary ./app-binary

ENV MONGODB_URI="mongodb://localhost:27017/default_db_local"
ENV MICRONAUT_SERVER_PORT=8080

EXPOSE 8080

# Execute the native machine code directly
ENTRYPOINT ["./app-binary"]