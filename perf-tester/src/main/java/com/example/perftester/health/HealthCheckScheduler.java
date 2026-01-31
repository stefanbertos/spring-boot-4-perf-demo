package com.example.perftester.health;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.jms.Connection;
import jakarta.jms.ConnectionFactory;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.sql.DriverManager;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Component
public class HealthCheckScheduler {

    private final ConnectionFactory mqConnectionFactory;
    private final MeterRegistry meterRegistry;
    private final String kafkaBootstrapServers;
    private final String oracleUrl;
    private final String oracleUser;
    private final String oraclePassword;
    private final int connectionTimeoutMs;

    private final AtomicInteger mqStatus = new AtomicInteger(0);
    private final AtomicInteger kafkaStatus = new AtomicInteger(0);
    private final AtomicInteger oracleStatus = new AtomicInteger(0);

    private final Timer mqPingTimer;
    private final Timer kafkaPingTimer;
    private final Timer oraclePingTimer;

    public HealthCheckScheduler(
            ConnectionFactory mqConnectionFactory,
            MeterRegistry meterRegistry,
            @Value("${app.healthcheck.kafka.bootstrap-servers:localhost:9092}") String kafkaBootstrapServers,
            @Value("${app.healthcheck.oracle.url:jdbc:oracle:thin:@localhost:1521/XEPDB1}") String oracleUrl,
            @Value("${app.healthcheck.oracle.user:perfuser}") String oracleUser,
            @Value("${app.healthcheck.oracle.password:perfpass}") String oraclePassword,
            @Value("${app.healthcheck.connection-timeout-ms:5000}") int connectionTimeoutMs) {

        this.mqConnectionFactory = mqConnectionFactory;
        this.meterRegistry = meterRegistry;
        this.kafkaBootstrapServers = kafkaBootstrapServers;
        this.oracleUrl = oracleUrl;
        this.oracleUser = oracleUser;
        this.oraclePassword = oraclePassword;
        this.connectionTimeoutMs = connectionTimeoutMs;

        Gauge.builder("health.mq.status", mqStatus, AtomicInteger::get)
                .description("IBM MQ connection status (1=up, 0=down)")
                .tag("service", "ibm-mq")
                .register(meterRegistry);

        Gauge.builder("health.kafka.status", kafkaStatus, AtomicInteger::get)
                .description("Kafka connection status (1=up, 0=down)")
                .tag("service", "kafka")
                .register(meterRegistry);

        Gauge.builder("health.oracle.status", oracleStatus, AtomicInteger::get)
                .description("Oracle DB connection status (1=up, 0=down)")
                .tag("service", "oracle")
                .register(meterRegistry);

        this.mqPingTimer = Timer.builder("health.ping.duration")
                .description("Health check ping duration")
                .tag("service", "ibm-mq")
                .register(meterRegistry);

        this.kafkaPingTimer = Timer.builder("health.ping.duration")
                .description("Health check ping duration")
                .tag("service", "kafka")
                .register(meterRegistry);

        this.oraclePingTimer = Timer.builder("health.ping.duration")
                .description("Health check ping duration")
                .tag("service", "oracle")
                .register(meterRegistry);

        log.info("Health check scheduler initialized - Kafka: {}, Oracle: {}", kafkaBootstrapServers, oracleUrl);
    }

    @Scheduled(fixedRateString = "${app.healthcheck.interval-ms:60000}")
    public void performHealthChecks() {
        log.debug("Performing health checks...");
        checkMq();
        checkKafka();
        checkOracle();
    }

    private void checkMq() {
        long startTime = System.nanoTime();
        try (Connection connection = mqConnectionFactory.createConnection()) {
            connection.start();
            mqStatus.set(1);
            long duration = System.nanoTime() - startTime;
            mqPingTimer.record(duration, TimeUnit.NANOSECONDS);
            log.debug("MQ health check passed in {}ms", TimeUnit.NANOSECONDS.toMillis(duration));
        } catch (Exception e) {
            mqStatus.set(0);
            long duration = System.nanoTime() - startTime;
            mqPingTimer.record(duration, TimeUnit.NANOSECONDS);
            log.warn("MQ health check failed: {}", e.getMessage());
        }
    }

    private void checkKafka() {
        long startTime = System.nanoTime();
        var props = Map.<String, Object>of(
                AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaBootstrapServers,
                AdminClientConfig.REQUEST_TIMEOUT_MS_CONFIG, connectionTimeoutMs,
                AdminClientConfig.DEFAULT_API_TIMEOUT_MS_CONFIG, connectionTimeoutMs
        );

        try (AdminClient adminClient = AdminClient.create(props)) {
            adminClient.listTopics().names().get(connectionTimeoutMs, TimeUnit.MILLISECONDS);
            kafkaStatus.set(1);
            long duration = System.nanoTime() - startTime;
            kafkaPingTimer.record(duration, TimeUnit.NANOSECONDS);
            log.debug("Kafka health check passed in {}ms", TimeUnit.NANOSECONDS.toMillis(duration));
        } catch (Exception e) {
            kafkaStatus.set(0);
            long duration = System.nanoTime() - startTime;
            kafkaPingTimer.record(duration, TimeUnit.NANOSECONDS);
            log.warn("Kafka health check failed: {}", e.getMessage());
        }
    }

    private void checkOracle() {
        long startTime = System.nanoTime();
        try {
            DriverManager.setLoginTimeout((int) Duration.ofMillis(connectionTimeoutMs).toSeconds());
            try (var connection = DriverManager.getConnection(oracleUrl, oracleUser, oraclePassword)) {
                connection.isValid((int) Duration.ofMillis(connectionTimeoutMs).toSeconds());
                oracleStatus.set(1);
                long duration = System.nanoTime() - startTime;
                oraclePingTimer.record(duration, TimeUnit.NANOSECONDS);
                log.debug("Oracle health check passed in {}ms", TimeUnit.NANOSECONDS.toMillis(duration));
            }
        } catch (Exception e) {
            oracleStatus.set(0);
            long duration = System.nanoTime() - startTime;
            oraclePingTimer.record(duration, TimeUnit.NANOSECONDS);
            log.warn("Oracle health check failed: {}", e.getMessage());
        }
    }
}
