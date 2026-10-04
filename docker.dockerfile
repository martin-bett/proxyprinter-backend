# Use the official Maven image with Java 25 as the base image
FROM --platform=$BUILDPLATFORM maven:3.9.11-eclipse-temurin-25 AS build

# Set the working directory
WORKDIR /app

# Copy the pom.xml and any other necessary configuration files
COPY ./pom.xml /app
COPY ./src /app/src

# Build the application
RUN mvn clean package -Dmaven.test.skip=true

# Create a new image for running the application
FROM eclipse-temurin:25-jre

# Set the working directory
WORKDIR /app

# Copy the built WAR file from the previous stage
COPY --from=build /app/target/mtgprint-0.0.1-SNAPSHOT.war app.war

# Expose the port on which the Spring Boot application will run
EXPOSE 8080

# Command to run the application
CMD ["java", "-jar", "app.war"]