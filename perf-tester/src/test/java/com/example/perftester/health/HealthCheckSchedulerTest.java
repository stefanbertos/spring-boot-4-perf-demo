package com.example.perftester.health;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.ServerSocket;

import static org.assertj.core.api.Assertions.assertThat;

class HealthCheckSchedulerTest {

    private MeterRegistry meterRegistry;
    private ServerSocket kafkaServer;
    private ServerSocket mqServer;
    private ServerSocket oracleServer;
    private ServerSocket redisServer;

    @BeforeEach
    void setUp() throws IOException {
        meterRegistry = new SimpleMeterRegistry();
        kafkaServer = new ServerSocket(0);
        mqServer = new ServerSocket(0);
        oracleServer = new ServerSocket(0);
        redisServer = new ServerSocket(0);
    }

    @AfterEach
    void tearDown() {
        closeServer(kafkaServer);
        closeServer(mqServer);
        closeServer(oracleServer);
        closeServer(redisServer);
    }

    private void closeServer(ServerSocket server) {
        if (server != null && !server.isClosed()) {
            try {
                server.close();
            } catch (IOException e) {
                // Ignore
            }
        }
    }

    private HealthCheckProperties createProperties(int kafkaPort, int mqPort, int oraclePort, int redisPort) {
        return new HealthCheckProperties(
                new HealthCheckProperties.ServiceEndpoint("localhost", kafkaPort),
                new HealthCheckProperties.ServiceEndpoint("localhost", mqPort),
                new HealthCheckProperties.ServiceEndpoint("localhost", oraclePort),
                new HealthCheckProperties.ServiceEndpoint("localhost", redisPort),
                5000,
                60000
        );
    }

    private HealthCheckScheduler createScheduler() {
        var properties = createProperties(
                kafkaServer.getLocalPort(),
                mqServer.getLocalPort(),
                oracleServer.getLocalPort(),
                redisServer.getLocalPort()
        );
        return new HealthCheckScheduler(properties, meterRegistry);
    }

    private HealthCheckScheduler createSchedulerWithBadPorts() {
        return new HealthCheckScheduler(
                new HealthCheckProperties(
                        new HealthCheckProperties.ServiceEndpoint("localhost", 1),
                        new HealthCheckProperties.ServiceEndpoint("localhost", 2),
                        new HealthCheckProperties.ServiceEndpoint("localhost", 3),
                        new HealthCheckProperties.ServiceEndpoint("localhost", 4),
                        1000,
                        60000
                ),
                meterRegistry
        );
    }

    @Test
    void constructorShouldRegisterGauges() {
        createScheduler();

        assertThat(meterRegistry.find("health.infra.status").tag("service", "kafka").gauge()).isNotNull();
        assertThat(meterRegistry.find("health.infra.status").tag("service", "ibm-mq").gauge()).isNotNull();
        assertThat(meterRegistry.find("health.infra.status").tag("service", "oracle").gauge()).isNotNull();
        assertThat(meterRegistry.find("health.infra.status").tag("service", "redis").gauge()).isNotNull();
    }

    @Test
    void constructorShouldRegisterTimers() {
        createScheduler();

        assertThat(meterRegistry.find("health.ping.duration").tag("service", "kafka").timer()).isNotNull();
        assertThat(meterRegistry.find("health.ping.duration").tag("service", "ibm-mq").timer()).isNotNull();
        assertThat(meterRegistry.find("health.ping.duration").tag("service", "oracle").timer()).isNotNull();
        assertThat(meterRegistry.find("health.ping.duration").tag("service", "redis").timer()).isNotNull();
    }

    @Test
    void performHealthChecksShouldSetStatusToOneWhenAllServicesUp() {
        var scheduler = createScheduler();
        scheduler.performHealthChecks();

        assertGaugeValue("kafka", 1.0);
        assertGaugeValue("ibm-mq", 1.0);
        assertGaugeValue("oracle", 1.0);
        assertGaugeValue("redis", 1.0);
    }

    @Test
    void checkKafkaShouldSetStatusToZeroOnFailure() throws IOException {
        kafkaServer.close();

        var properties = new HealthCheckProperties(
                new HealthCheckProperties.ServiceEndpoint("localhost", 1),
                new HealthCheckProperties.ServiceEndpoint("localhost", mqServer.getLocalPort()),
                new HealthCheckProperties.ServiceEndpoint("localhost", oracleServer.getLocalPort()),
                new HealthCheckProperties.ServiceEndpoint("localhost", redisServer.getLocalPort()),
                1000,
                60000
        );
        var scheduler = new HealthCheckScheduler(properties, meterRegistry);
        scheduler.performHealthChecks();

        assertGaugeValue("kafka", 0.0);
        assertGaugeValue("ibm-mq", 1.0);
        assertGaugeValue("oracle", 1.0);
        assertGaugeValue("redis", 1.0);
    }

    @Test
    void checkMqShouldSetStatusToZeroOnFailure() throws IOException {
        mqServer.close();

        var properties = new HealthCheckProperties(
                new HealthCheckProperties.ServiceEndpoint("localhost", kafkaServer.getLocalPort()),
                new HealthCheckProperties.ServiceEndpoint("localhost", 2),
                new HealthCheckProperties.ServiceEndpoint("localhost", oracleServer.getLocalPort()),
                new HealthCheckProperties.ServiceEndpoint("localhost", redisServer.getLocalPort()),
                1000,
                60000
        );
        var scheduler = new HealthCheckScheduler(properties, meterRegistry);
        scheduler.performHealthChecks();

        assertGaugeValue("kafka", 1.0);
        assertGaugeValue("ibm-mq", 0.0);
        assertGaugeValue("oracle", 1.0);
        assertGaugeValue("redis", 1.0);
    }

    @Test
    void checkOracleShouldSetStatusToZeroOnFailure() throws IOException {
        oracleServer.close();

        var properties = new HealthCheckProperties(
                new HealthCheckProperties.ServiceEndpoint("localhost", kafkaServer.getLocalPort()),
                new HealthCheckProperties.ServiceEndpoint("localhost", mqServer.getLocalPort()),
                new HealthCheckProperties.ServiceEndpoint("localhost", 3),
                new HealthCheckProperties.ServiceEndpoint("localhost", redisServer.getLocalPort()),
                1000,
                60000
        );
        var scheduler = new HealthCheckScheduler(properties, meterRegistry);
        scheduler.performHealthChecks();

        assertGaugeValue("kafka", 1.0);
        assertGaugeValue("ibm-mq", 1.0);
        assertGaugeValue("oracle", 0.0);
        assertGaugeValue("redis", 1.0);
    }

    @Test
    void checkRedisShouldSetStatusToZeroOnFailure() throws IOException {
        redisServer.close();

        var properties = new HealthCheckProperties(
                new HealthCheckProperties.ServiceEndpoint("localhost", kafkaServer.getLocalPort()),
                new HealthCheckProperties.ServiceEndpoint("localhost", mqServer.getLocalPort()),
                new HealthCheckProperties.ServiceEndpoint("localhost", oracleServer.getLocalPort()),
                new HealthCheckProperties.ServiceEndpoint("localhost", 4),
                1000,
                60000
        );
        var scheduler = new HealthCheckScheduler(properties, meterRegistry);
        scheduler.performHealthChecks();

        assertGaugeValue("kafka", 1.0);
        assertGaugeValue("ibm-mq", 1.0);
        assertGaugeValue("oracle", 1.0);
        assertGaugeValue("redis", 0.0);
    }

    @Test
    void healthChecksShouldRecordTimerMetrics() {
        var scheduler = createScheduler();
        scheduler.performHealthChecks();

        var kafkaTimer = meterRegistry.find("health.ping.duration").tag("service", "kafka").timer();
        var mqTimer = meterRegistry.find("health.ping.duration").tag("service", "ibm-mq").timer();
        var oracleTimer = meterRegistry.find("health.ping.duration").tag("service", "oracle").timer();
        var redisTimer = meterRegistry.find("health.ping.duration").tag("service", "redis").timer();

        assertThat(kafkaTimer).isNotNull();
        assertThat(kafkaTimer.count()).isEqualTo(1);
        assertThat(mqTimer).isNotNull();
        assertThat(mqTimer.count()).isEqualTo(1);
        assertThat(oracleTimer).isNotNull();
        assertThat(oracleTimer.count()).isEqualTo(1);
        assertThat(redisTimer).isNotNull();
        assertThat(redisTimer.count()).isEqualTo(1);
    }

    @Test
    void healthChecksShouldRecordTimerMetricsOnFailure() {
        var scheduler = createSchedulerWithBadPorts();
        scheduler.performHealthChecks();

        var kafkaTimer = meterRegistry.find("health.ping.duration").tag("service", "kafka").timer();
        var mqTimer = meterRegistry.find("health.ping.duration").tag("service", "ibm-mq").timer();
        var oracleTimer = meterRegistry.find("health.ping.duration").tag("service", "oracle").timer();
        var redisTimer = meterRegistry.find("health.ping.duration").tag("service", "redis").timer();

        assertThat(kafkaTimer).isNotNull();
        assertThat(kafkaTimer.count()).isEqualTo(1);
        assertThat(mqTimer).isNotNull();
        assertThat(mqTimer.count()).isEqualTo(1);
        assertThat(oracleTimer).isNotNull();
        assertThat(oracleTimer.count()).isEqualTo(1);
        assertThat(redisTimer).isNotNull();
        assertThat(redisTimer.count()).isEqualTo(1);
    }

    @Test
    void allServicesDownShouldSetAllStatusesToZero() {
        var scheduler = createSchedulerWithBadPorts();
        scheduler.performHealthChecks();

        assertGaugeValue("kafka", 0.0);
        assertGaugeValue("ibm-mq", 0.0);
        assertGaugeValue("oracle", 0.0);
        assertGaugeValue("redis", 0.0);
    }

    private void assertGaugeValue(String service, double expectedValue) {
        var gauge = meterRegistry.find("health.infra.status").tag("service", service).gauge();
        assertThat(gauge).isNotNull();
        assertThat(gauge.value()).isEqualTo(expectedValue);
    }
}
