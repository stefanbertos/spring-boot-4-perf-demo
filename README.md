# perf-demo

Multi-module Spring Boot application with IBM MQ integration for performance testing.

## Modules

| Module | Description | Port |
|--------|-------------|------|
| perf-tester | REST API to send messages, listens for processed responses | 8080 |
| consumer | Consumes messages from DEV.QUEUE.2, adds "processed" suffix, sends to DEV.QUEUE.1 | 8081 |

## Prerequisites

- Java 25
- Docker

## Running the Application

1. Start services (IBM MQ, Prometheus, Grafana):
   ```bash
   docker compose up -d
   ```

2. Run both modules:
   ```bash
   gradlew.bat :perf-tester:bootRun
   gradlew.bat :consumer:bootRun
   ```

## Message Flow

```
perf-tester -> DEV.QUEUE.2 -> consumer -> DEV.QUEUE.1 -> perf-tester
                              (adds "processed" suffix)
```

## IBM MQ Web Console

- URL: https://localhost:9443/ibmmq/console
- Username: `admin`
- Password: `passw0rd`

Note: The console uses a self-signed certificate, so you'll need to accept the browser warning.

## Prometheus

- URL: http://localhost:9090
- Metrics endpoint (perf-tester): http://localhost:8080/actuator/prometheus
- Metrics endpoint (consumer): http://localhost:8081/actuator/prometheus

## Grafana

- URL: http://localhost:3000
- Username: `admin`
- Password: `admin`

Dashboards are auto-provisioned:
- Spring Boot - perf-demo
- IBM MQ

## Swagger UI

- URL: http://localhost:8080/swagger-ui.html
- OpenAPI JSON: http://localhost:8080/v3/api-docs

## API Endpoints

### Send Message
```bash
curl -X POST http://localhost:8080/api/perf/send -d "your message"
```

## Queue Configuration

| Queue | Purpose |
|-------|---------|
| DEV.QUEUE.1 | perf-tester inbound / consumer outbound |
| DEV.QUEUE.2 | perf-tester outbound / consumer inbound |
