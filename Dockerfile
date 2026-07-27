# syntax=docker/dockerfile:1.7

FROM maven:3.9.10-eclipse-temurin-17 AS build

WORKDIR /workspace

ARG OTEL_JAVA_AGENT_VERSION=2.28.1

ADD "https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/download/v${OTEL_JAVA_AGENT_VERSION}/opentelemetry-javaagent.jar" /workspace/opentelemetry-javaagent.jar

COPY pom.xml .
COPY workflow-service/pom.xml workflow-service/pom.xml
COPY order-service/pom.xml order-service/pom.xml
COPY inventory-service/pom.xml inventory-service/pom.xml
COPY payment-service/pom.xml payment-service/pom.xml
COPY shipping-service/pom.xml shipping-service/pom.xml
COPY invoice-service/pom.xml invoice-service/pom.xml
COPY notification-service/pom.xml notification-service/pom.xml
COPY integration-tests/pom.xml integration-tests/pom.xml

COPY . .

RUN --mount=type=cache,target=/root/.m2 mvn -B \
    -pl workflow-service,order-service,inventory-service,payment-service,shipping-service,invoice-service,notification-service \
    -am package spring-boot:repackage -DskipTests -Dmaven.test.skip=true

FROM eclipse-temurin:17-jre-alpine

ARG SERVICE

RUN addgroup -S spring && adduser -S spring -G spring

WORKDIR /app

COPY --from=build "/workspace/${SERVICE}/target/${SERVICE}-0.0.1-SNAPSHOT.jar" app.jar
COPY --from=build /workspace/opentelemetry-javaagent.jar /otel/opentelemetry-javaagent.jar
COPY infrastructure/monitoring/otel-javaagent-config.yaml /otel/otel-javaagent-config.yaml

RUN chmod 0644 /otel/opentelemetry-javaagent.jar

ENV JAVA_TOOL_OPTIONS="-javaagent:/otel/opentelemetry-javaagent.jar -Dotel.config.file=/otel/otel-javaagent-config.yaml"

USER spring:spring

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
