# syntax=docker/dockerfile:1
# One Dockerfile for every service:  docker build --build-arg MODULE=customer-service -t homefin/customer-service .

# ---------- 1) build ----------
FROM maven:3.9-eclipse-temurin-21 AS build
ARG MODULE
WORKDIR /workspace
COPY . .
# -pl <module> -am : build only this module + the modules it depends on (common-lib)
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -q -pl ${MODULE} -am package -DskipTests

# ---------- 2) extract layers (dependencies change rarely -> better Docker layer caching) ----------
FROM eclipse-temurin:21-jre AS extract
ARG MODULE
WORKDIR /extract
COPY --from=build /workspace/${MODULE}/target/${MODULE}-*.jar app.jar
RUN java -Djarmode=tools -jar app.jar extract --layers --launcher --destination layers

# ---------- 3) runtime ----------
FROM eclipse-temurin:21-jre
RUN useradd --system --uid 1001 spring
USER spring
WORKDIR /app
COPY --from=extract /extract/layers/dependencies/ ./
COPY --from=extract /extract/layers/spring-boot-loader/ ./
COPY --from=extract /extract/layers/snapshot-dependencies/ ./
COPY --from=extract /extract/layers/application/ ./
# Container-aware JVM: size the heap from the container memory limit.
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError"
ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
