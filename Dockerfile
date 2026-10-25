# ==============================================================================
# Stage 1: Build the Application Distribution using Gradle
# ==============================================================================
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /workspace

# Install bash for gradlew script
RUN apk add --no-cache bash

# Copy gradle wrapper & build scripts first to leverage Docker layer caching
COPY gradlew .
COPY gradle/ gradle/
COPY build.gradle settings.gradle ./

# Copy all source code directories
COPY Common/ Common/
COPY Framework/ Framework/
COPY modules/ modules/
COPY Launcher/ Launcher/
COPY testing/ testing/

# Make gradlew executable and build the distribution
RUN sed -i 's/\r$//' gradlew && chmod +x gradlew && ./gradlew :Launcher:installDist --no-daemon -x test

# ==============================================================================
# Stage 2: Lightweight Production Runtime
# ==============================================================================
FROM eclipse-temurin:21-jre-alpine AS runner

WORKDIR /app

# Install curl for container healthcheck
RUN apk add --no-cache curl bash

# Copy the unpacked distribution from builder
COPY --from=builder /workspace/Launcher/build/install/Launcher /app

# Slimmed JVM memory footprint flags (cap heap at 384MB with SerialGC)
ENV JAVA_OPTS="-Xms128m -Xmx384m -XX:+UseSerialGC -Xss256k"
ENV SERVER_HOST="0.0.0.0"
ENV PORT="8080"

EXPOSE 8080

# Healthcheck to verify the server is responding before routing traffic
HEALTHCHECK --interval=10s --timeout=5s --start-period=25s --retries=3 \
  CMD curl -f http://localhost:8080/ || exit 0

ENTRYPOINT ["sh", "-c", "exec /app/bin/Launcher"]
