# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build Commands

```bash
# Build the project
./gradlew build

# Run the application
./gradlew bootRun

# Run all tests
./gradlew test

# Run a single test class
./gradlew test --tests "com.example.perfdemo.PerfDemoApplicationTests"

# Run a single test method
./gradlew test --tests "com.example.perfdemo.PerfDemoApplicationTests.contextLoads"

# Clean build
./gradlew clean build
```

## Architecture

This is a Spring Boot 4.0 web application with the following characteristics:

- **Java 25** with Gradle build system
- **Spring WebMVC** for REST endpoints
- **Spring Actuator** with Prometheus metrics export (`/actuator` endpoints)
- **Lombok** for boilerplate reduction
- **Docker Compose** support for dev services (IBM MQ in `compose.yaml`)
- **IBM MQ** with mq-jms-spring-boot-starter for JMS messaging

### Project Structure

- `src/main/java/com/example/perfdemo/` - Main application code
  - `PerfDemoApplication.java` - Spring Boot entry point
  - `rest/` - REST controllers
  - `messaging/` - JMS messaging components
    - `MessageListener.java` - Listens on inbound queue (DEV.QUEUE.1)
    - `MessageSender.java` - Sends to outbound queue (DEV.QUEUE.2)
- `src/test/java/` - JUnit 5 tests with `@SpringBootTest`
- `src/main/resources/application.yml` - Configuration including IBM MQ settings
