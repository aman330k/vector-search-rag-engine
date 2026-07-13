# ---- Build stage ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
# Cache dependencies first
COPY pom.xml .
RUN mvn -q -B dependency:go-offline
# Build the app
COPY src ./src
RUN mvn -q -B clean package -DskipTests

# ---- Run stage ----
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/vectordb-java-*.jar app.jar
# Persist the document snapshot outside the container by mounting /app/data
VOLUME ["/app/data"]
EXPOSE 8081
# Default: talk to Ollama on the host. Spring Boot relaxed binding maps this env var to
# vectordb.ollama.base-url automatically. Override with docker-compose or -e.
ENV VECTORDB_OLLAMA_BASEURL="http://host.docker.internal:11434"
ENTRYPOINT ["java", "-jar", "app.jar"]
