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

# Run all tests
./gradlew test

# Run tests for specific module
./gradlew :perf-tester:test
./gradlew :ibm-mq-consumer:test
./gradlew :kafka-consumer:test

# Clean build
./gradlew clean build
```

## Architecture

Multi-module Spring Boot 4.0 application for MQ and Kafka performance testing.

- **Java 25** with Gradle multi-module build
- **Spring Actuator** with Prometheus metrics export
- **Lombok** for boilerplate reduction
- **Docker Compose** for IBM MQ, Kafka, Prometheus, Grafana
- **IBM MQ** with mq-jms-spring-boot-starter:4.0.1
- **Kafka** with spring-boot-starter-kafka

### Modules

- **perf-tester** (port 8080) - REST API to send messages, receives processed responses
- **ibm-mq-consumer** (port 8081) - Bridges MQ to Kafka and back
- **kafka-consumer** (port 8082) - Processes Kafka messages, adds "processed" suffix

### Message Flow

```
perf-tester -> DEV.QUEUE.2 -> ibm-mq-consumer -> Kafka (mq-requests) -> kafka-consumer
                                                                              |
perf-tester <- DEV.QUEUE.1 <- ibm-mq-consumer <- Kafka (mq-responses) <-------+
```

## Coding Standards

### Java 25 Features (Required)

**Local Variable Type Inference** - Use `var` for local variables when the type is obvious:
```java
// Preferred
var messages = new ArrayList<String>();
var response = restClient.get().retrieve().body(String.class);
var config = applicationContext.getBean(AppConfig.class);

// Avoid when type is not clear
var result = process(data);  // Bad - unclear what type result is
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
- Use `@RequiredArgsConstructor` from Lombok only when constructor injection with many dependencies

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
- **No field injection** - Use constructor injection (see Spring Boot 4 section)
- **Naming conventions** - Classes: `UpperCamelCase`, methods/variables: `lowerCamelCase`, constants: `UPPER_SNAKE_CASE`
- **Braces required** - All `if`, `for`, `while` blocks must use braces
- **Method length** - Maximum 100 lines per method
- **Parameter count** - Maximum 7 parameters per method
- **@Override required** - Always annotate overridden methods
- **Equals/hashCode** - Must override together
- **No trailing whitespace** - Lines must not end with whitespace
- **Newline at EOF** - Files must end with a newline

### PMD Rules Enforced

PMD 7.20.0 provides additional code analysis:

- **Best Practices** - Guard statements, unused variables/methods
- **Code Style** - Naming conventions, unnecessary code
- **Design** - Complexity limits, coupling analysis
- **Error Prone** - Common programming mistakes, resource management
- **Performance** - StringBuilder usage, loop optimizations
- **Security** - Security vulnerability detection
- **Multithreading** - Thread safety issues

**Excluded rules** (aligned with CLAUDE.md standards):
- `UseExplicitTypes` - Allow `var` keyword (Java 25 feature)
- `AvoidCatchingGenericException` - Allow for observability/span recording
- `LooseCoupling` - Spring beans often use concrete types
- `DoNotUseThreads` - Not a J2EE application

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
