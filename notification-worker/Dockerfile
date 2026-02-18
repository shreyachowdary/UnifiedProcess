# Multi-stage: build then run
FROM eclipse-temurin:17-jdk-alpine AS builder
WORKDIR /build

COPY pom.xml .
COPY notification-worker/pom.xml notification-worker/
COPY sms-simulator/pom.xml sms-simulator/

RUN apk add --no-cache maven && \
    mvn dependency:go-offline -B -pl notification-worker -am || true

COPY . .
RUN mvn package -B -pl notification-worker -am -DskipTests

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=builder /build/notification-worker/target/notification-worker-*.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
