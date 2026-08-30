# Build stage
FROM maven:3.9.6-eclipse-temurin-17-alpine AS build
WORKDIR /app

# Copy pom.xml and resolve dependencies
COPY pom.xml .
RUN mvn dependency:resolve-plugins -B && \
    mvn dependency:resolve -B

# Copy source code and build
COPY src ./src
RUN mvn package -DskipTests -B && ls -lh target/

# Runtime stage
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Create unprivileged application user
RUN addgroup -S spring && adduser -S spring -G spring

# Copy built jar from build stage
COPY --from=build --chown=spring:spring /app/target/discount-service.jar /app/discount-service.jar

# Switch to non-root user
USER spring:spring

EXPOSE 7005

# Container-aware JVM options
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:InitialRAMPercentage=50.0 -XX:+ExitOnOutOfMemoryError"

# Container healthcheck using Spring Actuator
HEALTHCHECK --interval=30s --timeout=5s --start-period=30s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:7005/actuator/health/liveness || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/discount-service.jar"]

