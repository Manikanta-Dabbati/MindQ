# Stage 1: Build
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app

# Copy Maven wrapper and pom.xml first for caching
COPY pom.xml .
COPY .mvn .mvn
COPY mvnw .
RUN chmod +x mvnw

# Download dependencies (cached layer)
RUN ./mvnw dependency:go-offline -B

# Copy source and build
COPY src ./src
RUN ./mvnw package -DskipTests -B

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-alpine AS runtime
WORKDIR /app

# Create non-root user
RUN addgroup -S mindq && adduser -S mindq -G mindq

# Copy JAR from build stage
COPY --from=build /app/target/*.jar app.jar

# Create data directory
RUN mkdir -p /app/data && chown -R mindq:mindq /app

USER mindq

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
