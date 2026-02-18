# Multi-stage: build then run
FROM eclipse-temurin:17-jdk-alpine AS builder
WORKDIR /build

COPY pom.xml .
COPY notification-api/pom.xml notification-api/

# Download dependencies (cached layer)
RUN apk add --no-cache maven && \
    mvn dependency:go-offline -B -pl notification-api -am || true

COPY . .
RUN mvn package -B -pl notification-api -am -DskipTests

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=builder /build/notification-api/target/notification-api-*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
