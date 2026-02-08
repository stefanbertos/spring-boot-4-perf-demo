FROM eclipse-temurin:25-jre

WORKDIR /app

COPY build/libs/*.jar app.jar

EXPOSE 8080

ENV JAVA_TOOL_OPTIONS="-XX:+UseZGC -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:InitialRAMPercentage=75.0"

ENTRYPOINT ["java", "-jar", "app.jar"]
