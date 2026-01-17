# perf-demo

Spring Boot application with IBM MQ integration for performance testing.

## Prerequisites

- Java 25
- Docker

## Running the Application

1. Start services (IBM MQ, Prometheus, Grafana):
   ```bash
   docker compose up -d
   ```

2. Run the application:
   ```bash
   ./gradlew bootRun
   ```

## IBM MQ Web Console

- URL: https://localhost:9443/ibmmq/console
- Username: `admin`
- Password: `passw0rd`

Note: The console uses a self-signed certificate, so you'll need to accept the browser warning.

## Prometheus

- URL: http://localhost:9090
- Metrics endpoint: http://localhost:8080/actuator/prometheus

## Grafana

- URL: http://localhost:3000
- Username: `admin`
- Password: `admin`

To add Prometheus as a data source:
1. Go to Connections > Data sources
2. Add Prometheus with URL: `http://prometheus:9090`

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
| DEV.QUEUE.1 | Inbound (listener reads from here) |
| DEV.QUEUE.2 | Outbound (sender writes here) |
