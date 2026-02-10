# Spring Boot 4 Performance Demo

Multi-module Spring Boot 4.0 application for MQ and Kafka performance testing with full observability stack.

## Architecture

```
perf-tester -> DEV.QUEUE.2 -> ibm-mq-consumer -> Kafka (mq-requests) -> kafka-consumer
                                                                              |
                                                                    (adds "processed" suffix)
                                                                              |
perf-tester <- DEV.QUEUE.1 <- ibm-mq-consumer <- Kafka (mq-responses) <-------+
```

## Modules

| Module | Description | Port |
|--------|-------------|------|
| perf-tester | REST API to send messages, listens for processed responses | 8080 |
| ibm-mq-consumer | Bridges MQ to Kafka and back | 8081 |
| kafka-consumer | Processes Kafka messages, adds "processed" suffix | 8082 |
| api-gateway | Routes all services through a single endpoint | 8090 |
| config-server | Spring Cloud Config Server with filesystem backend | 8888 |

## Tech Stack

- **Java 25** with virtual threads
- **Spring Boot 4.0** with Spring Cloud Gateway
- **Spring Cloud Config Server** with filesystem backend
- **Gradle** multi-module build with Checkstyle and PMD
- **Cloud Native Buildpacks** (Paketo) for OCI image builds via `bootBuildImage`
- **IBM MQ** with mq-jms-spring-boot-starter
- **Apache Kafka** for message streaming
- **Oracle Database** for persistence
- **Redis** for in-memory data store
- **Observability**: Prometheus, Grafana, Loki, Tempo
- **Code Quality**: SonarQube

## Prerequisites

- Java 25
- Docker & Docker Compose
- Kubernetes cluster (for Helm deployment)
- Helm 3.x (for Kubernetes deployment)

## Quick Start

### Local Development (Docker Compose)

1. Build OCI images and start all services:
   ```bash
   ./gradlew bootBuildImage
   docker compose up -d
   ```

2. Or use the convenience script (builds, creates images, starts everything):
   ```bash
   runLocalDocker.bat
   ```

3. Or run infrastructure only and start apps locally:
   ```bash
   docker compose up -d redis kafka schema-registry ibm-mq oracle prometheus grafana loki tempo
   ```

   Then run each module (start config-server first):
   ```bash
   ./gradlew :config-server:bootRun
   ./gradlew :perf-tester:bootRun
   ./gradlew :ibm-mq-consumer:bootRun
   ./gradlew :kafka-consumer:bootRun
   ./gradlew :api-gateway:bootRun
   ```

### Kubernetes Deployment (Helm)

```bash
cd infrastructure/helm
deployHelm.bat
```

To uninstall:
```bash
cd infrastructure/helm
cleanup.bat
```

## Service URLs

### Via API Gateway (Kubernetes with Ingress)

| Service | URL | Credentials |
|---------|-----|-------------|
| Grafana | http://localhost/grafana | admin / admin |
| Prometheus | http://localhost/prometheus | - |
| Kafdrop | http://localhost/kafdrop | - |
| Redis Commander | http://localhost/redis-commander | - |
| Loki | http://localhost/loki | - |
| Tempo | http://localhost/tempo | - |
| IBM MQ Console | http://localhost/ibmmq | admin / passw0rd |
| SonarQube | http://localhost/sonar | admin / admin |
| Config Server | http://localhost/config | - |
| Swagger UI | http://localhost/api/swagger-ui/index.html | - |
| API Docs | http://localhost/api/v3/api-docs | - |

### Direct Access (Docker Compose)

| Service | URL | Credentials |
|---------|-----|-------------|
| Grafana | http://localhost:3000 | admin / admin |
| Prometheus | http://localhost:9090 | - |
| Kafdrop | http://localhost:9000/kafdrop | - |
| Redis Commander | http://localhost:8083/redis-commander | - |
| Loki | http://localhost:3100 | - |
| Tempo | http://localhost:3200 | - |
| IBM MQ Console | https://localhost:9443/ibmmq/console | admin / passw0rd |
| SonarQube | http://localhost:9001 | admin / admin |
| Config Server | http://localhost:8888 | - |
| Swagger UI | http://localhost:8080/swagger-ui/index.html | - |
| Redis | localhost:6379 | - |
| Oracle DB | localhost:1521/XEPDB1 | perfuser / perfpass |

## API Endpoints

### Send Message
```bash
curl -X POST http://localhost:8080/api/perf/send -d "your message"
```

### Admin Endpoints

**Logging** - Change log levels at runtime (defaults to `com.example` package):
```bash
# Set log level
curl -X POST "http://localhost:8080/api/admin/logging/level?level=DEBUG"
curl -X POST "http://localhost:8080/api/admin/logging/level?loggerName=org.springframework&level=DEBUG"

# Get current log level
curl "http://localhost:8080/api/admin/logging/level"
curl "http://localhost:8080/api/admin/logging/level?loggerName=org.springframework"
```

**Kafka** - Resize Kafka topic partitions:
```bash
# Resize topic
curl -X POST "http://localhost:8080/api/admin/kafka/topics/resize?topicName=mq-requests&partitions=10"

# Get topic info
curl "http://localhost:8080/api/admin/kafka/topics?topicName=mq-requests"
```

**IBM MQ** - Change queue max depth:
```bash
# Set max queue depth
curl -X POST "http://localhost:8080/api/admin/mq/queues/depth?queueName=DEV.QUEUE.1&maxDepth=100000"

# Get queue info
curl "http://localhost:8080/api/admin/mq/queues?queueName=DEV.QUEUE.1"
```

### Health Check
```bash
curl http://localhost:8080/actuator/health
```

### Infrastructure Health Monitoring

The perf-tester module performs TCP port health checks on infrastructure services:

| Service | Port | Metric |
|---------|------|--------|
| Kafka | 9092 | `health.infra.status{service="kafka"}` |
| IBM MQ | 1414 | `health.infra.status{service="ibm-mq"}` |
| Oracle | 1521 | `health.infra.status{service="oracle"}` |
| Redis | 6379 | `health.infra.status{service="redis"}` |

Health check timing is exposed via `health.ping.duration` metric.

Configuration (application.yml):
```yaml
app:
  healthcheck:
    kafka:
      host: kafka
      port: 9092
    mq:
      host: ibm-mq
      port: 1414
    redis:
      host: redis
      port: 6379
    oracle:
      host: oracle
      port: 1521
    connection-timeout-ms: 5000
    interval-ms: 60000
```

### Metrics
```bash
curl http://localhost:8080/actuator/prometheus
```

## Centralized Configuration

All application configuration is centralized via Spring Cloud Config Server with a filesystem backend.

### Config Repository Structure

```
config-repo/
├── application.yml              # Shared config (virtual threads, task pool, management)
├── application-docker.yml       # Shared Docker Compose profile
├── application-kubernetes.yml   # Shared Kubernetes profile
├── perf-tester.yml             # perf-tester specific config
├── perf-tester-docker.yml
├── perf-tester-kubernetes.yml
├── ibm-mq-consumer.yml         # ibm-mq-consumer specific config
├── kafka-consumer.yml          # kafka-consumer specific config
├── api-gateway.yml             # api-gateway routes and config
└── ...
```

### Accessing Config Server

```bash
# Get config for an application
curl http://localhost:8888/perf-tester/default
curl http://localhost:8888/perf-tester/docker
curl http://localhost:8888/application/default
```

## Queue & Topic Configuration

### IBM MQ Queues

| Queue | Purpose |
|-------|---------|
| DEV.QUEUE.1 | perf-tester inbound / ibm-mq-consumer outbound |
| DEV.QUEUE.2 | perf-tester outbound / ibm-mq-consumer inbound |

### Kafka Topics

| Topic | Purpose |
|-------|---------|
| mq-requests | ibm-mq-consumer outbound / kafka-consumer inbound |
| mq-responses | kafka-consumer outbound / ibm-mq-consumer inbound |

## Build Commands

### Build Profiles

The build supports two profiles for different deployment targets:

| Profile | Command | Description |
|---------|---------|-------------|
| `docker` (default) | `./gradlew build` | Docker Compose support enabled, localhost URLs |
| `kubernetes` | `./gradlew build -Pprofile=kubernetes` | No docker-compose, Kubernetes service URLs |

```bash
# Show current build profile
./gradlew showProfile

# Build with default (docker) profile
./gradlew build

# Build for Kubernetes/Helm deployment
./gradlew build -Pprofile=kubernetes

# Run with Kubernetes profile (uses application-kubernetes.yml)
./gradlew :perf-tester:bootRun -Pprofile=kubernetes
```

### Standard Commands

```bash
# Build all modules (includes documentation generation)
./gradlew build

# Build OCI images with Cloud Native Buildpacks
./gradlew bootBuildImage

# Run tests
./gradlew test

# Run specific module
./gradlew :perf-tester:bootRun
./gradlew :ibm-mq-consumer:bootRun
./gradlew :kafka-consumer:bootRun
./gradlew :api-gateway:bootRun

# Clean build
./gradlew clean build

# Run static analysis only
./gradlew checkstyleMain pmdMain

# Generate documentation only
./gradlew generateDocs
```

## Documentation

Technical documentation is automatically generated as part of the build process in both PDF and HTML formats.

### Generated Documentation

| Format | Location | Description |
|--------|----------|-------------|
| PDF | `build/docs/pdf/index.pdf` | Complete technical documentation |
| HTML | `build/docs/html/index.html` | Web-browsable documentation |

### Documentation Chapters

1. **Architecture Overview** - System design, modules, message flow
2. **Installation Guide** - Prerequisites, setup, verification
3. **Configuration** - Application settings, environment variables
4. **API Reference** - REST endpoints, metrics, health checks
5. **Deployment** - Docker Compose, Kubernetes, GCP
6. **Observability** - Metrics, logging, tracing
7. **Troubleshooting** - Common issues and solutions
8. **Appendix** - Quick reference, commands, glossary

## Infrastructure Components

### Helm Charts

| Chart | Description |
|-------|-------------|
| api-gateway | Spring Cloud Gateway for routing |
| config-server | Spring Cloud Config Server with filesystem backend |
| grafana | Dashboards and visualization |
| ibm-mq | IBM MQ queue manager |
| ibm-mq-consumer | MQ to Kafka bridge service |
| ingress | Kubernetes ingress configuration |
| kafdrop | Kafka web UI |
| kafka | Apache Kafka broker |
| kafka-consumer | Kafka message processor |
| kafka-exporter | Kafka metrics for Prometheus |
| loki | Log aggregation |
| oracle | Oracle XE database |
| oracle-exporter | Oracle metrics for Prometheus |
| redis | Redis in-memory data store |
| redis-commander | Redis web UI |
| perf-tester | Main REST API service |
| prometheus | Metrics collection |
| promtail | Log shipping to Loki |
| sonarqube | Code quality analysis |
| tempo | Distributed tracing |

## Observability

### Grafana Dashboards

Pre-configured dashboards for:
- Spring Boot application metrics
- IBM MQ queue depths and message rates
- Kafka consumer lag and throughput
- Oracle database performance
- JVM metrics (heap, GC, threads)

### Distributed Tracing

All services are instrumented with Micrometer Tracing, sending traces to Tempo. View traces in Grafana.

### Log Aggregation

Logs are collected by Promtail and stored in Loki. Query logs in Grafana using LogQL.

## Debugging

### Enable Debug Logging

At runtime (no restart needed):
```bash
curl -X POST "http://localhost:8080/api/admin/logging/level?loggerName=org.springframework.jms&level=DEBUG"
curl -X POST "http://localhost:8080/api/admin/logging/level?loggerName=com.ibm.mq&level=DEBUG"
```

At startup:
```bash
./gradlew :perf-tester:bootRun --args='--logging.level.org.springframework.jms=DEBUG --logging.level.com.ibm.mq=DEBUG'
```

Or in application.yaml:
```yaml
logging:
  level:
    org.springframework.jms: DEBUG
    com.ibm.mq: DEBUG
```

## Static Analysis

Build enforces **Checkstyle** and **PMD 7.20.0**:

```bash
# View reports after build
# Checkstyle: build/reports/checkstyle/main.html
# PMD: build/reports/pmd/main.html
```

## SonarQube Integration

### Exposing SonarQube

**Docker Compose** — SonarQube is available at `http://localhost:9001/sonar` (mapped from container port 9000).

**Kubernetes** — Port-forward the SonarQube pod to access it locally:

```bash
kubectl port-forward svc/perf-sonarqube 9001:9000 -n perf-demo
```

SonarQube is then available at `http://localhost:9001/sonar`.

Via Ingress / API Gateway: `http://localhost/sonar`

### Running SonarQube Analysis

```bash
# With Docker Compose (default)
./gradlew sonar

# With Kubernetes (port-forward SonarQube first, then run)
./gradlew sonar -Pprofile=kubernetes

# With custom URL and token
SONAR_HOST_URL=http://your-sonar:9000 SONAR_TOKEN=your-token ./gradlew sonar
```

### Helm Deployment with Sonar Token

When deploying SonarQube via Helm, configure the authentication token:

```bash
# Deploy with token
helm install perf-sonarqube ./sonarqube --set sonar.token=your-generated-token

# Or update existing deployment
helm upgrade perf-sonarqube ./sonarqube --set sonar.token=your-generated-token
```

The token is stored in a Kubernetes Secret (`perf-sonarqube-token`) and can be used by CI/CD pipelines:

```bash
# Retrieve token from secret
kubectl get secret perf-sonarqube-token -o jsonpath='{.data.token}' | base64 -d
```

## GCP/GKE Deployment

For Google Kubernetes Engine:

```bash
set GCP_PROJECT=your-project-id
set GCP_REGION=us-central1
cd infrastructure/helm
deployHelm.bat
```

Images are automatically pushed to Google Artifact Registry.