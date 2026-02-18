# Stage 1: Build
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Run
FROM eclipse-temurin:21-jre
WORKDIR /app

RUN groupadd -r eventara && useradd -r -g eventara eventara

COPY --from=build /app/target/*.jar app.jar
RUN chown eventara:eventara app.jar

USER eventara
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
