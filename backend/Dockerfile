# --- Build ---
FROM eclipse-temurin:21-jdk-jammy AS build
WORKDIR /app

COPY mvnw mvnw.cmd ./
COPY .mvn .mvn
COPY pom.xml .
COPY src src

RUN chmod +x mvnw && ./mvnw -B package -DskipTests

# --- Run (small JRE image) ---
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

RUN useradd --system --uid 1000 --no-create-home appuser

COPY --from=build /app/target/project-0.0.1-SNAPSHOT.jar app.jar

USER appuser

# Platforms set PORT; Spring uses server.port=${PORT:8000}
EXPOSE 8000
ENV PORT=8000

ENTRYPOINT ["java", "-jar", "app.jar"]
