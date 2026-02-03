FROM eclipse-temurin:25-jre

WORKDIR /app

COPY build/libs/*.jar app.jar

EXPOSE 8888

ENTRYPOINT ["java", "-XX:+UseZGC", "-jar", "app.jar"]
