# Stage 1: Build
FROM eclipse-temurin:21-jdk-alpine AS build

WORKDIR /app

# Install Maven
RUN apk add --no-cache maven

# Copy Maven configuration first for better layer caching
COPY pom.xml .

# Download dependencies
RUN mvn dependency:go-offline -B

# Copy source
COPY src ./src

# Build application
RUN mvn package -DskipTests -B

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-alpine AS runtime

WORKDIR /app

# Create non-root user
RUN addgroup -S mindq && adduser -S mindq -G mindq

# Copy built JAR
COPY --from=build /app/target/*.jar app.jar

# Application data directory
RUN mkdir -p /app/data && chown -R mindq:mindq /app

USER mindq

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]