FROM eclipse-temurin:25-jre

RUN apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/*

WORKDIR /app

COPY build/libs/*.jar app.jar

EXPOSE 8888

ENV JAVA_TOOL_OPTIONS="-XX:+UseZGC -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:InitialRAMPercentage=75.0"

ENTRYPOINT ["java", "-jar", "app.jar"]
