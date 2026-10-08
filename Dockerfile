
# Stage 1: Build the Spring Boot application
FROM maven:3.9.9-eclipse-temurin-21 AS build

WORKDIR /app

# Copy Maven configuration and download dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy application source code and build the JAR
COPY src ./src
RUN mvn -B -DskipTests clean package

# Stage 2: Run the application
FROM eclipse-temurin:21-jre

WORKDIR /app

# Copy the generated JAR and give it a consistent name
COPY --from=build /app/target/*.jar /app/app.jar

# Render supplies the PORT environment variable
EXPOSE 10000

ENTRYPOINT ["sh", "-c", "exec java -Dserver.port=${PORT:-10000} -jar /app/app.jar"]

