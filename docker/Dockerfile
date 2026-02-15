# Multi-stage Dockerfile for Quarkus JVM mode
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /app

# Copy parent POM and module POMs
COPY pom.xml ./
COPY common/pom.xml ./common/
COPY chat-service/pom.xml ./chat-service/
COPY admin-service/pom.xml ./admin-service/

# Download dependencies
RUN mvn dependency:go-offline -B

# Copy source code
COPY common/src ./common/src
COPY chat-service/src ./chat-service/src
COPY admin-service/src ./admin-service/src

# Build the applications
RUN mvn clean package -DskipTests -B

# Runtime stage
FROM registry.access.redhat.com/ubi9/openjdk-21-runtime:1.18

ENV LANGUAGE='en_US:en'

# Copy the built artifacts
COPY --from=build --chown=185 /app/chat-service/target/quarkus-app/lib/ /deployments/lib/
COPY --from=build --chown=185 /app/chat-service/target/quarkus-app/*.jar /deployments/
COPY --from=build --chown=185 /app/chat-service/target/quarkus-app/app/ /deployments/app/
COPY --from=build --chown=185 /app/chat-service/target/quarkus-app/quarkus/ /deployments/quarkus/

# Configure
EXPOSE 8080
USER 185

ENV JAVA_OPTS="-Dquarkus.http.host=0.0.0.0 -Djava.util.logging.manager=org.jboss.logmanager.LogManager"
ENV JAVA_APP_JAR="/deployments/quarkus-run.jar"

ENTRYPOINT [ "java", "-jar", "/deployments/quarkus-run.jar" ]
