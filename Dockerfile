# ===== Stage 1: Build the dashboard =====
FROM node:20-alpine AS dashboard-build
WORKDIR /app/eventara-dashboard
COPY eventara-dashboard/package*.json ./
RUN npm ci --production=false
COPY eventara-dashboard/ .
# Vite builds to ../src/main/resources/static (relative to eventara-dashboard)
RUN npm run build

# ===== Stage 2: Build the Spring Boot app =====
FROM maven:3.9.9-eclipse-temurin-21 AS backend-build
WORKDIR /app
COPY pom.xml .
COPY src ./src
# Copy dashboard build output into Spring Boot static resources
COPY --from=dashboard-build /app/src/main/resources/static ./src/main/resources/static/
RUN mvn clean package -DskipTests

# ===== Stage 3: Run the app =====
FROM eclipse-temurin:21-jre
WORKDIR /app

# Run as non-root
RUN groupadd -r eventara && useradd -r -g eventara eventara

COPY --from=backend-build /app/target/*.jar app.jar
RUN chown eventara:eventara app.jar

USER eventara
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
