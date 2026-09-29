# syntax=docker/dockerfile:1
# One Dockerfile for every service:  docker build --build-arg MODULE=customer-service -t homefin/customer-service .
# Multi-stage build: each FROM starts a new stage; only the last stage becomes the final image,
# so Maven, the JDK and the source code never ship to production (like a Node build stage that
# runs "npm run build" and a slim runtime stage that copies only dist/ and node_modules).

# ---------- 1) build ----------
# Maven + full JDK 21 (compiler) image.
FROM maven:3.9-eclipse-temurin-21 AS build
# Build argument: which Maven module to package (passed by docker-compose.yml "args").
ARG MODULE
WORKDIR /workspace
COPY . .
# -pl <module> -am : build only this module + the modules it depends on (common-lib)
# -B = batch (non-interactive) mode, -q = quiet, skipTests = tests run in CI, not in the image build.
# The cache mount keeps the downloaded Maven repository (~/.m2, like the npm cache) between builds.
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -q -pl ${MODULE} -am package -DskipTests

# ---------- 2) extract layers (dependencies change rarely -> better Docker layer caching) ----------
# JRE only (runtime, no compiler) - smaller image.
FROM eclipse-temurin:21-jre AS extract
ARG MODULE
WORKDIR /extract
# The spring-boot-maven-plugin produced an executable "fat JAR" in target/.
COPY --from=build /workspace/${MODULE}/target/${MODULE}-*.jar app.jar
# Spring Boot's jarmode "tools" splits the fat JAR into folders: third-party dependencies, the Spring Boot
# loader, SNAPSHOT dependencies (e.g. common-lib) and our application classes.
RUN java -Djarmode=tools -jar app.jar extract --layers --launcher --destination layers

# ---------- 3) runtime ----------
FROM eclipse-temurin:21-jre
# Run as an unprivileged system user instead of root.
RUN useradd --system --uid 1001 spring
USER spring
WORKDIR /app
# Copy from least to most frequently changing, so a code-only change rebuilds just the last layer.
COPY --from=extract /extract/layers/dependencies/ ./
COPY --from=extract /extract/layers/spring-boot-loader/ ./
COPY --from=extract /extract/layers/snapshot-dependencies/ ./
COPY --from=extract /extract/layers/application/ ./
# Container-aware JVM: size the heap from the container memory limit.
# ExitOnOutOfMemoryError: crash fast so the orchestrator restarts the container.
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError"
# Start via Spring Boot's launcher class, which loads the extracted layers from the classpath
# (equivalent to "java -jar app.jar", but for the exploded layout).
ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
