# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build Commands

```bash
# Build all modules
./gradlew build

# Run perf-tester module
./gradlew :perf-tester:bootRun

# Run ibm-mq-consumer module
./gradlew :ibm-mq-consumer:bootRun

# Run kafka-consumer module
./gradlew :kafka-consumer:bootRun

# Run config-server module
./gradlew :config-server:bootRun

# Run all tests
./gradlew test

# Run tests for specific module
./gradlew :perf-tester:test
./gradlew :ibm-mq-consumer:test
./gradlew :kafka-consumer:test
./gradlew :config-server:test

# Clean build
./gradlew clean build
```

## Architecture

Multi-module Spring Boot 4.0 application for MQ and Kafka performance testing.

- **Java 25** with Gradle multi-module build
- **Spring Cloud Config Server** with filesystem backend for centralized configuration
- **Spring Actuator** with Prometheus metrics export
- **Lombok** for boilerplate reduction
- **Docker Compose** for IBM MQ, Kafka, Prometheus, Grafana
- **IBM MQ** with mq-jms-spring-boot-starter:4.0.1
- **Kafka** with spring-boot-starter-kafka

### Modules

- **config-server** (port 8888) - Spring Cloud Config Server with filesystem backend
- **perf-tester** (port 8080) - REST API to send messages, receives processed responses
- **ibm-mq-consumer** (port 8081) - Bridges MQ to Kafka and back
- **kafka-consumer** (port 8082) - Processes Kafka messages, adds "processed" suffix
- **api-gateway** (port 8090) - Spring Cloud Gateway for routing all services

### Message Flow

```
perf-tester -> DEV.QUEUE.2 -> ibm-mq-consumer -> Kafka (mq-requests) -> kafka-consumer
                                                                              |
perf-tester <- DEV.QUEUE.1 <- ibm-mq-consumer <- Kafka (mq-responses) <-------+
```

## Coding Standards

### Java 25 Features (Required)

**Local Variable Type Inference** - Always use `var` for local variables when the type is obvious from the right-hand side. This includes constructor calls, factory methods, builder patterns, getter methods, and for-each loops:
```java
// Preferred - type obvious from RHS
var messages = new ArrayList<String>();
var response = restClient.get().retrieve().body(String.class);
var config = applicationContext.getBean(AppConfig.class);
var span = tracer.spanBuilder("operation").startSpan();
var body = message.getText();

// For-each loops - always use var
for (var entry : collection) { ... }

// Try-with-resources - use var
try (var scope = span.makeCurrent()) { ... }

// Avoid when type is not clear
var result = process(data);  // Bad - unclear what type result is

// Cannot use var with null assignment
String value = null;  // OK - var not possible here
```

**Records for DTOs** - Use records for immutable data carriers:
```java
// Use records for request/response DTOs
public record MessageRequest(String content, String correlationId) {}
public record MessageResponse(String content, String status, Instant timestamp) {}

// Use records for configuration properties
@ConfigurationProperties(prefix = "app.messaging")
public record MessagingProperties(String queueName, int timeout, boolean enabled) {}
```

**Pattern Matching** - Use pattern matching for instanceof and switch:
```java
// Pattern matching for instanceof
if (event instanceof MessageEvent messageEvent) {
    process(messageEvent.payload());
}

// Pattern matching in switch
return switch (status) {
    case Status.PENDING -> "Waiting";
    case Status.COMPLETE -> "Done";
    case Status.ERROR error -> "Failed: " + error.message();
};
```

**Text Blocks** - Use text blocks for multi-line strings:
```java
var query = """
    SELECT id, name, status
    FROM messages
    WHERE created_at > :since
    ORDER BY created_at DESC
    """;
```

**Sealed Classes** - Use sealed classes for restricted hierarchies:
```java
public sealed interface MessageResult permits Success, Failure {
    record Success(String data) implements MessageResult {}
    record Failure(String error, Exception cause) implements MessageResult {}
}
```

### Spring Boot 4 Features (Required)

**Constructor Injection** - Always use constructor injection, never field injection:
```java
// Correct - constructor injection (implicit with single constructor)
@Service
public class MessageService {
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final MeterRegistry meterRegistry;

    public MessageService(KafkaTemplate<String, String> kafkaTemplate, MeterRegistry meterRegistry) {
        this.kafkaTemplate = kafkaTemplate;
        this.meterRegistry = meterRegistry;
    }
}

// Avoid - field injection
@Service
public class MessageService {
    @Autowired  // Bad
    private KafkaTemplate<String, String> kafkaTemplate;
}
```

**Declarative HTTP Clients** - Use `@HttpExchange` for REST clients:
```java
@HttpExchange("/api/v1")
public interface ExternalApiClient {
    @GetExchange("/messages/{id}")
    MessageResponse getMessage(@PathVariable String id);

    @PostExchange("/messages")
    MessageResponse createMessage(@RequestBody MessageRequest request);
}
```

**Virtual Threads** - Leverage virtual threads for blocking operations:
```java
// application.properties
spring.threads.virtual.enabled=true

// Or programmatically for specific executors
@Bean
public ExecutorService virtualThreadExecutor() {
    return Executors.newVirtualThreadPerTaskExecutor();
}
```

**RestClient over RestTemplate** - Use the new RestClient API:
```java
@Bean
public RestClient restClient(RestClient.Builder builder) {
    return builder
        .baseUrl("http://api.example.com")
        .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
        .build();
}

// Usage
var response = restClient.get()
    .uri("/messages/{id}", id)
    .retrieve()
    .body(MessageResponse.class);
```

**Observability** - Use Micrometer annotations for metrics and tracing:
```java
@Observed(name = "message.process", contextualName = "process-message")
public void processMessage(String message) {
    // Method is automatically traced and metered
}
```

**Problem Details (RFC 7807)** - Return structured error responses:
```java
@ExceptionHandler(MessageNotFoundException.class)
public ProblemDetail handleNotFound(MessageNotFoundException ex) {
    var problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    problem.setTitle("Message Not Found");
    problem.setProperty("messageId", ex.getMessageId());
    return problem;
}
```

### General Guidelines

- Prefer immutability: use `final` fields, records, and unmodifiable collections
- Use `Optional` for return types that may be absent, never for parameters or fields
- Prefer `List.of()`, `Set.of()`, `Map.of()` for immutable collections
- Use `@Nullable` and `@NonNull` annotations from Spring for null safety
- Avoid Lombok `@Data` - prefer records or explicit getters for mutable classes

**`@RequiredArgsConstructor` (Required)** - Always use `@RequiredArgsConstructor` from Lombok for constructor injection when the constructor only assigns parameters to final fields. Do NOT use it when the constructor has `@Value` annotations, builds objects, or contains any logic beyond simple field assignment:
```java
// USE @RequiredArgsConstructor - constructor only assigns to final fields
@Service
@RequiredArgsConstructor
public class OrderService {
    private final OrderRepository orderRepository;
    private final NotificationService notificationService;
}

// DO NOT use @RequiredArgsConstructor - constructor has @Value
@Service
public class EmailService {
    private final String smtpHost;
    private final RestClient restClient;

    public EmailService(@Value("${smtp.host}") String smtpHost) {
        this.smtpHost = smtpHost;
        this.restClient = RestClient.builder().baseUrl(smtpHost).build();
    }
}
```

## Static Analysis

The build enforces coding standards using **Checkstyle** (source-level) and **PMD 7.20.0** (code analysis).

```bash
# Build runs both checkstyle and PMD automatically (fails before JAR on violations)
./gradlew build

# Run only static analysis
./gradlew checkstyleMain pmdMain

# View reports after build
# Checkstyle: build/reports/checkstyle/main.html
# PMD: build/reports/pmd/main.html
```

**Build Order:** Compile → Checkstyle → PMD → JAR (no artifacts if checks fail)

### Checkstyle Rules Enforced

The following rules are enforced (build fails on violations):

- **No wildcard imports** - Import specific classes
- **No unused imports** - Remove unused imports
- **No field injection** - `@Autowired`, `@Inject`, and `@Resource` on fields are forbidden; use constructor injection
- **No Lombok `@Data`** - Use records for DTOs or explicit getters for mutable classes
- **Naming conventions** - Classes: `UpperCamelCase`, methods/variables: `lowerCamelCase`, constants: `UPPER_SNAKE_CASE`
- **Braces required** - All `if`, `for`, `while` blocks must use braces
- **Method length** - Maximum 100 lines per method
- **Parameter count** - Maximum 7 parameters per method
- **File length** - Maximum 500 lines per file
- **@Override required** - Always annotate overridden methods
- **Equals/hashCode** - Must override together
- **No trailing whitespace** - Lines must not end with whitespace
- **Newline at EOF** - Files must end with a newline

### PMD Rules Enforced

PMD 7.20.0 provides additional code analysis:

- **Best Practices** - Unused variables/methods, method argument checks
- **Code Style** - Naming conventions, unnecessary code
- **Design** - Coupling analysis, exception handling patterns
- **Error Prone** - Common programming mistakes, resource management
- **Performance** - StringBuilder usage, loop optimizations
- **Security** - Security vulnerability detection
- **Multithreading** - Thread safety issues
- **Documentation** - Comment placement

**Excluded rules** (aligned with project standards):
- `UseExplicitTypes` - Allow `var` keyword (Java 25 feature)
- `AvoidCatchingGenericException` - Allow for observability/span recording
- `LooseCoupling` - Spring beans often use concrete types
- `DoNotUseThreads` - Not a J2EE application
- `GuardLogStatement` - SLF4J parameterized logging handles this
- `AvoidThrowingRawExceptionTypes` - Allowed for infrastructure error wrapping
- `DataClass` - Records and Lombok classes trigger false positives
- `LawOfDemeter` - Fluent APIs and builder patterns require method chaining

### Project Structure

```
perf-demo/
├── build.gradle              # Root build with shared config
├── settings.gradle           # Module includes
├── compose.yaml              # Docker Compose orchestration
├── config-repo/              # Spring Cloud Config files (served by config-server)
│   ├── application.yml       # Shared config for all services
│   ├── application-kubernetes.yml
│   ├── ibm-mq-consumer.yml
│   ├── ibm-mq-consumer-kubernetes.yml
│   ├── kafka-consumer.yml
│   ├── kafka-consumer-kubernetes.yml
│   ├── perf-tester.yml
│   └── perf-tester-kubernetes.yml
├── infrastructure/
│   ├── helm/                 # Helm charts for Kubernetes deployment
│   │   ├── config-server/    # Includes ConfigMap with all service configs
│   │   ├── ibm-mq-consumer/
│   │   ├── kafka-consumer/
│   │   ├── perf-tester/
│   │   ├── schema-registry/
│   │   └── ...               # kafka, ibm-mq, grafana, prometheus, etc.
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
├── ibm-mq-consumer/
│   ├── build.gradle
│   └── src/main/java/com/example/ibmmqconsumer/
│       ├── IbmMqConsumerApplication.java
│       └── messaging/
│           ├── MqMessageListener.java      # DEV.QUEUE.2 -> Kafka
│           └── KafkaResponseListener.java  # Kafka -> DEV.QUEUE.1
└── kafka-consumer/
    ├── build.gradle
    └── src/main/java/com/example/kafkaconsumer/
        ├── KafkaConsumerApplication.java
        └── messaging/
            └── KafkaRequestListener.java  # Kafka request -> Kafka response
```

## Architecture Guidelines

### Layered Architecture (Required)

Maintain a strict **Controller → Service → Repository** flow:

- **Controller**: HTTP request handling, input validation, orchestration — no business logic
- **Service**: Business rules, transaction boundaries, data transformation
- **Repository**: Persistence logic only

Never inject repositories directly into controllers — always go through a service layer:
```java
// Bad - repository in controller, skips service layer
@RestController
public class UserController {
    private final UserRepository userRepository;
    // ...
    @GetMapping("/users/{id}")
    public User getUser(@PathVariable Long id) {
        return userRepository.findById(id).orElseThrow();
    }
}

// Good - controller delegates to service
@RestController
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @GetMapping("/users/{id}")
    public UserResponse getUser(@PathVariable Long id) {
        return userService.getUserById(id);
    }
}
```

### Separate DTOs for Request, Entity, and Response (Required)

JPA entities must never double as API models. Entities change for persistence reasons, DTOs change for API contract reasons:
```java
// Request DTO - use records with validation
public record CreateUserRequest(
    @NotBlank String name,
    @Email String email
) {}

// Response DTO - use records
public record UserResponse(String name, String email) {}

// Entity - separate from DTOs
@Entity
public class User {
    @Id @GeneratedValue
    private Long id;
    private String name;
    private String email;
}
```

### Configuration Class Organization (Required)

Break configuration into focused `@Configuration` classes grouped by concern:
```java
@Configuration
public class SecurityConfig { /* security beans */ }

@Configuration
public class MessagingConfig { /* Kafka, MQ beans */ }

@Configuration
public class CachingConfig { /* cache managers */ }
```

### Input Validation (Required)

Validate at boundaries (controllers, API endpoints), not in the core service layer. Use `@Valid` with Bean Validation annotations on request DTOs:
```java
@PostMapping("/users")
public ResponseEntity<Void> createUser(@Valid @RequestBody CreateUserRequest request) {
    userService.createUser(request);
    return ResponseEntity.status(HttpStatus.CREATED).build();
}
```

The service layer can then assume inputs are valid and non-null.

### Global Exception Handling (Required)

Use `@ControllerAdvice` for centralized error handling with Problem Details (RFC 7807):
```java
@ControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(UserNotFoundException.class)
    public ProblemDetail handleNotFound(UserNotFoundException ex) {
        var problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setTitle("User Not Found");
        return problem;
    }
}
```

### Meaningful HTTP Status Codes (Required)

Use semantically correct HTTP status codes:
- `201 Created` for resource creation
- `204 No Content` for successful deletes
- `400 Bad Request` for invalid input
- `404 Not Found` for missing resources
- `409 Conflict` for state conflicts

### Transaction Boundaries (Required)

Apply `@Transactional` only where atomicity is required — not as a blanket annotation on every service method. Prefer read-only transactions for queries:
```java
@Transactional(readOnly = true)
public UserResponse getUserById(Long id) { ... }

@Transactional
public void transferFunds(Long from, Long to, BigDecimal amount) { ... }
```

### Avoid Static Utility Classes in Business Logic

Static methods hide dependencies and make unit testing difficult. Prefer Spring-managed beans so dependencies can be injected and mocked.

## Performance Guidelines

### JVM Tuning

All JVM tuning is applied via the `JAVA_TOOL_OPTIONS` environment variable, which the JVM reads automatically. This allows each deployment layer (Dockerfile, Docker Compose, Helm) to override options independently.

**Garbage Collector Selection:**
- **ZGC** (`-XX:+UseZGC`) — used in this project for low-latency messaging workloads
- **G1GC** (`-XX:+UseG1GC`) — alternative for balanced workloads with lower memory overhead
- **Shenandoah** (`-XX:+UseShenandoahGC`) — alternative for ultra-low latency

**Heap Configuration — Percentage-Based (Required for Containers):**
```
-XX:MaxRAMPercentage=75.0 -XX:InitialRAMPercentage=75.0
```
- Use `MaxRAMPercentage`/`InitialRAMPercentage` instead of fixed `-Xms`/`-Xmx` — heap auto-scales when container memory limits change
- Setting `InitialRAMPercentage` equal to `MaxRAMPercentage` avoids heap resizing overhead (equivalent to `-Xms == -Xmx`)
- 75% leaves room for metaspace, native memory, thread stacks, and off-heap buffers

**Fixed heap alternative** (when precise control is needed):
```
-Xms512m -Xmx512m
```
- Must be kept in sync with container memory limits manually

**Container Awareness:**
```
-XX:+UseContainerSupport
```
- Enables JVM to detect container memory/CPU limits (default since Java 10, explicit for clarity)
- The JVM respects cgroup limits for heap sizing and GC thread count

**Standard JAVA_TOOL_OPTIONS for this project:**
```
JAVA_TOOL_OPTIONS="-XX:+UseZGC -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:InitialRAMPercentage=75.0"
```

**Where JVM options are configured:**

| Layer | Location | Purpose |
|-------|----------|---------|
| Dockerfile | `ENV JAVA_TOOL_OPTIONS="..."` | Default for all container runs |
| Docker Compose | `environment: JAVA_TOOL_OPTIONS: "..."` | Override for local Docker Compose |
| Helm values.yaml | `jvm.options: "..."` | Override for Kubernetes deployments |
| Helm deployment.yaml | `env: JAVA_TOOL_OPTIONS` from values | Injected into pod containers |
| build.gradle | `bootRun.jvmArgs` | Local development via `./gradlew bootRun` |

**When changing JVM options, update all layers** (follows the Configuration Consistency Checklist).

**ZGC Memory Requirements (Required):**
- ZGC uses multi-mapped memory regions and has higher non-heap overhead than G1GC
- **Minimum 1Gi container memory limit** when using ZGC with `MaxRAMPercentage=75.0`
- With 75% heap, the remaining 25% must cover metaspace (~100Mi), thread stacks (~50Mi), code cache (~50Mi), and GC structures
- On a 512Mi container, 75% heap = ~384Mi leaves only ~128Mi for non-heap — **this causes OOM kills with ZGC**
- For containers under 1Gi, either increase the memory limit or lower `MaxRAMPercentage` to 50%

### Caching Strategy

Databases and external services are the biggest bottlenecks at scale. Use layered caching:

- **In-memory caching (Caffeine)**: For local, near-cache performance
- **Distributed caching (Redis)**: For multi-node environments
- **HTTP response caching**: For static or slowly changing responses
- **Database query caching**: Hibernate second-level cache or custom strategies

**Rule:** Never hit the database on every request when the data can be cached.

### Database Optimization

**Connection Pooling (HikariCP)** - tune for throughput:
```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 200
      minimum-idle: 50
      idle-timeout: 30000
      max-lifetime: 600000
```

**Best Practices:**
- Prefer batch inserts/updates over single queries
- Offload analytical queries to read replicas
- Use appropriate indexes for query patterns
- Consider NoSQL for ultra-high write/read workloads

### Serialization Optimization

Serialization is a silent performance killer at scale:

- **Prefer binary formats** (Avro, Protobuf) when both producer and consumer support it — this project uses **Apache Avro** for Kafka messages
- Use **Jackson Afterburner** to speed up JSON serialization where JSON is required
- Only enable response compression when the network is the bottleneck — compression consumes CPU

## Configuration Management

### Configuration Layers (Required)

This project uses **Spring Cloud Config Server** for centralized configuration. Configuration exists in three layers that must be kept in sync:

1. **Local `application.yml`** (in each module's `src/main/resources/`) — baseline defaults for local development
2. **Config-repo files** (`config-repo/<service>.yml`) — served by config-server, used in Docker Compose
3. **Helm ConfigMap** (`infrastructure/helm/config-server/templates/configmap.yaml`) — deployed in Kubernetes

**When changing any configuration property (serializers, connection strings, feature flags, etc.), update ALL THREE layers:**

| Layer | Path | Used When |
|-------|------|-----------|
| Local | `<module>/src/main/resources/application.yml` | Local dev without config-server |
| Config-repo | `config-repo/<service>.yml` | Docker Compose with config-server |
| Helm ConfigMap | `infrastructure/helm/config-server/templates/configmap.yaml` | Kubernetes / Rancher deployment |

Profile-specific overrides (`config-repo/<service>-kubernetes.yml`) handle Kubernetes-specific values like service hostnames and environment variable references.

### Logging Configuration (Required)

Default logging levels are configured in the shared `application.yml` (config-repo and Helm ConfigMap) so all apps inherit them:

```yaml
logging:
  level:
    root: INFO
    com.example: INFO
    org.springframework: INFO
    org.springframework.web: INFO
    org.apache.kafka: WARN
    io.confluent: WARN
    com.ibm.mq: INFO
    com.ibm.msg: INFO
    io.micrometer: WARN
    org.hibernate: WARN
```

**Rules:**
- Default logging is in `config-repo/application.yml` (shared by all apps) and mirrored in the Helm ConfigMap
- Each module's local `application.yml` also has logging as a fallback for running without config-server
- Service-specific overrides go in `config-repo/<service>.yml` (e.g., `com.example.perftester: DEBUG`)
- To enable debug for all apps, change `com.example: DEBUG` in the shared `application.yml`
- To enable debug for one app, change its level in the service-specific config file
- Noisy libraries (Kafka, Confluent, Hibernate, Micrometer) default to WARN to reduce log volume
- When changing log levels, follow the Configuration Consistency Checklist (update all 3 layers)

### Configuration Property Naming (Required)

Configuration properties bound to `@ConfigurationProperties` records **must match the record field names exactly**. Spring Boot uses relaxed binding, but the YAML keys must correspond to the Java field names:

```java
// Record expects host + port
public record ServiceEndpoint(String host, int port) {}
```

```yaml
# Correct - matches field names
app:
  healthcheck:
    kafka:
      host: perf-kafka
      port: 9092

# Wrong - Spring cannot bind these to ServiceEndpoint
app:
  healthcheck:
    kafka:
      bootstrap-servers: perf-kafka:9092  # No matching field
```

### Configuration Consistency Checklist

Before considering a configuration change complete, verify:
- [ ] Local `application.yml` has the correct values for local development
- [ ] `config-repo/<service>.yml` has the correct values for Docker Compose
- [ ] `config-repo/<service>-kubernetes.yml` has Kubernetes-specific overrides if needed
- [ ] Helm ConfigMap has the correct values for Kubernetes deployment — **both base and kubernetes profile files**
- [ ] Profile-specific files (`*-kubernetes.yml`) in ConfigMap match `config-repo/` versions
- [ ] Environment variables in Helm `deployment.yaml` templates match what the config expects

## Kubernetes / Helm Guidelines

### Resource Requests and Limits (Required)

All Kubernetes workloads (Deployments, DaemonSets, StatefulSets) **must** define resource requests and limits in their `values.yaml`:

```yaml
resources:
  requests:
    memory: "512Mi"
    cpu: "250m"
  limits:
    memory: "1Gi"
    cpu: "500m"
```

**Rules:**
- Always specify both `requests` and `limits` for `memory` and `cpu`
- Requests should be set to typical usage, limits to maximum acceptable
- **Minimum 1Gi memory limit** for Spring Boot services using ZGC (see JVM Tuning section)
- Memory limits prevent OOM kills; CPU limits prevent resource starvation
- Use appropriate units: memory in `Mi`/`Gi`, CPU in `m` (millicores) or cores

**Deployment template must reference resources:**
```yaml
containers:
  - name: {{ .Chart.Name }}
    resources:
      {{- toYaml .Values.resources | nindent 12 }}
```

### Health Probes (Required)

All Spring Boot deployments **must** define liveness and readiness probes with appropriate timing:

```yaml
livenessProbe:
  httpGet:
    path: /actuator/health/liveness
    port: http
  initialDelaySeconds: 60
  periodSeconds: 10
  failureThreshold: 5
readinessProbe:
  httpGet:
    path: /actuator/health/readiness
    port: http
  initialDelaySeconds: 30
  periodSeconds: 5
  failureThreshold: 10
```

**Rules:**
- Always set `failureThreshold` explicitly — the Kubernetes default (3) is too aggressive for Spring Boot startup
- `initialDelaySeconds` for liveness must be >= 60s (Spring Boot + ZGC initialization takes time)
- Readiness `failureThreshold` should be higher than liveness to allow the app to become ready without being killed
- Services with more dependencies (MQ, Kafka, DB) need longer initial delays (e.g., 90s for ibm-mq-consumer)
- The application must enable Kubernetes health probes in its configuration:
  ```yaml
  management:
    endpoint:
      health:
        probes:
          enabled: true
    health:
      livenessstate:
        enabled: true
      readinessstate:
        enabled: true
  ```

### Config Server ConfigMap (Required)

The config-server Helm ConfigMap (`infrastructure/helm/config-server/templates/configmap.yaml`) must include **all** configuration files that exist in `config-repo/`:

- Base files: `application.yml`, `<service>.yml` for each service
- Kubernetes profile files: `application-kubernetes.yml`, `<service>-kubernetes.yml` for each service

Client apps run with `SPRING_PROFILES_ACTIVE=kubernetes`, so the config-server must serve both `<service>.yml` and `<service>-kubernetes.yml`. Missing profile files means Kubernetes-specific overrides (service hostnames, env var references) won't be applied.

**When adding a new config-repo file, always add it to the Helm ConfigMap as well.**

### Deploy/Cleanup Script Consistency (Required)

The Helm deployment and cleanup scripts must stay in sync with the Helm charts:

- **`infrastructure/helm/deployHelm.bat`** — deploys all Helm charts in dependency order
- **`infrastructure/helm/cleanup.bat`** — uninstalls all Helm charts in reverse dependency order

**When adding a new Helm chart, always:**
1. Add a deploy step in `deployHelm.bat` at the correct position (respect dependency order)
2. Add an uninstall step in `cleanup.bat` (reverse order of deploy)
3. Update step numbering in both scripts (`[N/total]` counters)

**Deploy order rules:**
- Infrastructure first (databases, message brokers, monitoring)
- Kafka → Schema Registry → Config Server (strict dependency chain)
- Applications after Config Server (they depend on it for configuration)
- UI tools and exporters last (Kafdrop, Redis Commander, Kafka Exporter)
- Ingress last (routes to all services)

**Cleanup order rules:**
- Reverse of deploy order: Ingress first, infrastructure last
- Applications before Config Server
- Schema Registry before Kafka
- PVCs, ConfigMaps/Secrets, and namespace deleted at the very end

**Step numbering:**
- Use sequential `[N/total]` format — no gaps, no duplicates, no fractional steps (e.g., `12b`)
- Update the total count in all steps when adding/removing components

### Helm Chart Structure

Each service in `infrastructure/helm/` follows this structure:
```
service-name/
├── Chart.yaml           # Chart metadata
├── values.yaml          # Default configuration (must include resources)
└── templates/
    ├── _helpers.tpl     # Template helpers
    ├── deployment.yaml  # Deployment with resource specs
    ├── service.yaml     # Service definition
    └── ...              # Other resources (configmap, secret, pvc)
```
