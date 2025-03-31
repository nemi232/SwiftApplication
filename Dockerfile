# Build stage
FROM maven:3.9-eclipse-temurin-17-alpine AS build
WORKDIR /app
COPY pom.xml .
# Download all required dependencies
RUN mvn dependency:go-offline
COPY src ./src
# Package the application
RUN mvn package -DskipTests

# Run stage
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Add a non-root user
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Copy the JAR file from the build stage
COPY --from=build /app/target/*.jar app.jar

# Copy the Excel file if needed
# COPY Interns_2025_SWIFT_CODES.xlsx .

# Set entry point
ENTRYPOINT ["java", "-jar", "app.jar"]

# Expose the port
EXPOSE 8080