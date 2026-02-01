# perf-demo

Multi-module Spring Boot application with IBM MQ and Kafka integration for performance testing.

## Modules

| Module | Description | Port |
|--------|-------------|------|
| perf-tester | REST API to send messages, listens for processed responses | 8080 |
| ibm-mq-consumer | Consumes MQ messages, publishes to Kafka, receives Kafka responses, sends to MQ | 8081 |
| kafka-consumer | Consumes Kafka requests, processes (adds "processed" suffix), sends Kafka responses | 8082 |

## Prerequisites

- Java 25
- Docker

## Running the Application

1. Start services (IBM MQ, Kafka, Prometheus, Grafana):
   ```bash
   runLocalDocker.bat
   ```

2. Run all modules locally:
   ```bash
   gradlew.bat :perf-tester:bootRun
   gradlew.bat :ibm-mq-consumer:bootRun
   gradlew.bat :kafka-consumer:bootRun
   gradlew.bat :api-gateway:bootRun
   ```

## Message Flow

```
perf-tester -> DEV.QUEUE.2 -> ibm-mq-consumer -> Kafka (mq-requests) -> kafka-consumer
                                                                              |
                                                                    (adds "processed" suffix)
                                                                              |
perf-tester <- DEV.QUEUE.1 <- ibm-mq-consumer <- Kafka (mq-responses) <-------+
```

## IBM MQ Web Console

- URL: https://localhost:9443/ibmmq/console
- Username: `admin`
- Password: `passw0rd`

Note: The console uses a self-signed certificate, so you'll need to accept the browser warning.

## Prometheus

- URL: http://localhost:9090
- Metrics endpoint (perf-tester): http://localhost:8080/actuator/prometheus
- Metrics endpoint (ibm-mq-consumer): http://localhost:8081/actuator/prometheus
- Metrics endpoint (kafka-consumer): http://localhost:8082/actuator/prometheus

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
| DEV.QUEUE.1 | perf-tester inbound / ibm-mq-consumer outbound |
| DEV.QUEUE.2 | perf-tester outbound / ibm-mq-consumer inbound |

## Kafka Topics

| Topic | Purpose |
|-------|---------|
| mq-requests | ibm-mq-consumer outbound / kafka-consumer inbound |
| mq-responses | kafka-consumer outbound / ibm-mq-consumer inbound |

## Debugging MQ Messages

To enable detailed JMS/MQ logging, uncomment the debug levels in `application.yml`:

```yaml
logging:
  level:
    org.springframework.jms: DEBUG
    com.ibm.mq: DEBUG
    com.ibm.msg: DEBUG
```

Or pass as command-line arguments:
```bash
gradlew.bat :perf-tester:bootRun --args='--logging.level.org.springframework.jms=DEBUG --logging.level.com.ibm.mq=DEBUG'
```

This will log message headers, properties, and routing information.

## Production Deployment
```bash
cd infrastructure/helm
deployHelm.bat
```
