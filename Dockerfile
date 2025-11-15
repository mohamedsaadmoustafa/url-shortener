# Use Eclipse Temurin 21 JDK
FROM eclipse-temurin:21-jdk
# Temporary folder
VOLUME /tmp
# Specify the jar file (built by Maven)
ARG JAR_FILE=target/*.jar
# Copy the jar file into the image
COPY ${JAR_FILE} app.jar
# Run the jar
ENTRYPOINT ["java","-XX:+UseContainerSupport","-XX:MaxRAMPercentage=75.0","-jar","/app.jar"]
