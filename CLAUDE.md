# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build Commands

```bash
# Build all modules
./gradlew build

# Run perf-tester module
./gradlew :perf-tester:bootRun

# Run consumer module
./gradlew :consumer:bootRun

# Run all tests
./gradlew test

# Run tests for specific module
./gradlew :perf-tester:test
./gradlew :consumer:test

# Clean build
./gradlew clean build
```

## Architecture

Multi-module Spring Boot 4.0 application for MQ performance testing.

- **Java 25** with Gradle multi-module build
- **Spring Actuator** with Prometheus metrics export
- **Lombok** for boilerplate reduction
- **Docker Compose** for IBM MQ, Prometheus, Grafana
- **IBM MQ** with mq-jms-spring-boot-starter:4.0.1

### Modules

- **perf-tester** (port 8080) - REST API to send messages, receives processed responses
- **consumer** (port 8081) - Processes messages, adds "processed" suffix

### Message Flow

```
perf-tester -> DEV.QUEUE.2 -> consumer -> DEV.QUEUE.1 -> perf-tester
```

### Project Structure

```
perf-demo/
├── build.gradle              # Root build with shared config
├── settings.gradle           # Module includes
├── compose.yaml              # Docker Compose orchestration
├── infrastructure/
│   ├── grafana/              # Grafana dashboards and provisioning
│   ├── mq-config/            # IBM MQ MQSC scripts
│   └── prometheus/           # Prometheus config
├── perf-tester/
│   ├── build.gradle
│   └── src/main/java/com/example/perftester/
│       ├── PerfTesterApplication.java
│       ├── rest/PerfController.java
│       └── messaging/
│           ├── MessageSender.java    # Sends to DEV.QUEUE.2
│           └── MessageListener.java  # Listens on DEV.QUEUE.1
└── consumer/
    ├── build.gradle
    └── src/main/java/com/example/consumer/
        ├── ConsumerApplication.java
        └── messaging/
            └── MessageProcessor.java  # DEV.QUEUE.2 -> DEV.QUEUE.1
```
