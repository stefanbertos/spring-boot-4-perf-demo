package com.example.perftester.health;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import jakarta.jms.Connection;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.JMSException;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.ListTopicsResult;
import org.apache.kafka.common.KafkaFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HealthCheckSchedulerTest {

    @Mock
    private ConnectionFactory mqConnectionFactory;

    @Mock
    private Connection mqConnection;

    private MeterRegistry meterRegistry;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
    }

    private HealthCheckScheduler createScheduler() {
        return new HealthCheckScheduler(
                mqConnectionFactory,
                meterRegistry,
                "localhost:9092",
                "jdbc:oracle:thin:@localhost:1521/XEPDB1",
                "testuser",
                "testpass",
                1000
        );
    }

    @Test
    void constructorShouldRegisterGauges() {
        try (var adminClientMock = mockStatic(AdminClient.class);
             var driverManagerMock = mockStatic(DriverManager.class)) {

            var scheduler = createScheduler();

            assertThat(meterRegistry.find("health.mq.status").gauge()).isNotNull();
            assertThat(meterRegistry.find("health.kafka.status").gauge()).isNotNull();
            assertThat(meterRegistry.find("health.oracle.status").gauge()).isNotNull();
        }
    }

    @Test
    void constructorShouldRegisterTimers() {
        try (var adminClientMock = mockStatic(AdminClient.class);
             var driverManagerMock = mockStatic(DriverManager.class)) {

            var scheduler = createScheduler();

            assertThat(meterRegistry.find("health.ping.duration").tag("service", "ibm-mq").timer()).isNotNull();
            assertThat(meterRegistry.find("health.ping.duration").tag("service", "kafka").timer()).isNotNull();
            assertThat(meterRegistry.find("health.ping.duration").tag("service", "oracle").timer()).isNotNull();
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    void performHealthChecksShouldCheckAllServices() throws Exception {
        try (var adminClientMock = mockStatic(AdminClient.class);
             var driverManagerMock = mockStatic(DriverManager.class)) {

            var scheduler = createScheduler();

            when(mqConnectionFactory.createConnection()).thenReturn(mqConnection);
            setupKafkaSuccess(adminClientMock);
            setupOracleSuccess(driverManagerMock);

            scheduler.performHealthChecks();

            verify(mqConnection).start();
            verify(mqConnection).close();
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    void checkMqShouldSetStatusToOneOnSuccess() throws Exception {
        try (var adminClientMock = mockStatic(AdminClient.class);
             var driverManagerMock = mockStatic(DriverManager.class)) {

            var scheduler = createScheduler();

            when(mqConnectionFactory.createConnection()).thenReturn(mqConnection);
            setupKafkaFailure(adminClientMock);
            setupOracleFailure(driverManagerMock);

            scheduler.performHealthChecks();

            var mqGauge = meterRegistry.find("health.mq.status").gauge();
            assertThat(mqGauge).isNotNull();
            assertThat(mqGauge.value()).isEqualTo(1.0);
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    void checkMqShouldSetStatusToZeroOnFailure() throws Exception {
        try (var adminClientMock = mockStatic(AdminClient.class);
             var driverManagerMock = mockStatic(DriverManager.class)) {

            var scheduler = createScheduler();

            when(mqConnectionFactory.createConnection()).thenThrow(new JMSException("Connection failed"));
            setupKafkaFailure(adminClientMock);
            setupOracleFailure(driverManagerMock);

            scheduler.performHealthChecks();

            var mqGauge = meterRegistry.find("health.mq.status").gauge();
            assertThat(mqGauge).isNotNull();
            assertThat(mqGauge.value()).isEqualTo(0.0);
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    void checkKafkaShouldSetStatusToOneOnSuccess() throws Exception {
        try (var adminClientMock = mockStatic(AdminClient.class);
             var driverManagerMock = mockStatic(DriverManager.class)) {

            var scheduler = createScheduler();

            when(mqConnectionFactory.createConnection()).thenThrow(new JMSException("MQ down"));
            setupKafkaSuccess(adminClientMock);
            setupOracleFailure(driverManagerMock);

            scheduler.performHealthChecks();

            var kafkaGauge = meterRegistry.find("health.kafka.status").gauge();
            assertThat(kafkaGauge).isNotNull();
            assertThat(kafkaGauge.value()).isEqualTo(1.0);
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    void checkKafkaShouldSetStatusToZeroOnFailure() throws Exception {
        try (var adminClientMock = mockStatic(AdminClient.class);
             var driverManagerMock = mockStatic(DriverManager.class)) {

            var scheduler = createScheduler();

            when(mqConnectionFactory.createConnection()).thenThrow(new JMSException("MQ down"));
            setupKafkaFailure(adminClientMock);
            setupOracleFailure(driverManagerMock);

            scheduler.performHealthChecks();

            var kafkaGauge = meterRegistry.find("health.kafka.status").gauge();
            assertThat(kafkaGauge).isNotNull();
            assertThat(kafkaGauge.value()).isEqualTo(0.0);
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    void checkOracleShouldSetStatusToOneOnSuccess() throws Exception {
        try (var adminClientMock = mockStatic(AdminClient.class);
             var driverManagerMock = mockStatic(DriverManager.class)) {

            var scheduler = createScheduler();

            when(mqConnectionFactory.createConnection()).thenThrow(new JMSException("MQ down"));
            setupKafkaFailure(adminClientMock);
            setupOracleSuccess(driverManagerMock);

            scheduler.performHealthChecks();

            var oracleGauge = meterRegistry.find("health.oracle.status").gauge();
            assertThat(oracleGauge).isNotNull();
            assertThat(oracleGauge.value()).isEqualTo(1.0);
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    void checkOracleShouldSetStatusToZeroOnFailure() throws Exception {
        try (var adminClientMock = mockStatic(AdminClient.class);
             var driverManagerMock = mockStatic(DriverManager.class)) {

            var scheduler = createScheduler();

            when(mqConnectionFactory.createConnection()).thenThrow(new JMSException("MQ down"));
            setupKafkaFailure(adminClientMock);
            setupOracleFailure(driverManagerMock);

            scheduler.performHealthChecks();

            var oracleGauge = meterRegistry.find("health.oracle.status").gauge();
            assertThat(oracleGauge).isNotNull();
            assertThat(oracleGauge.value()).isEqualTo(0.0);
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    void healthChecksShouldRecordTimerMetrics() throws Exception {
        try (var adminClientMock = mockStatic(AdminClient.class);
             var driverManagerMock = mockStatic(DriverManager.class)) {

            var scheduler = createScheduler();

            when(mqConnectionFactory.createConnection()).thenReturn(mqConnection);
            setupKafkaSuccess(adminClientMock);
            setupOracleSuccess(driverManagerMock);

            scheduler.performHealthChecks();

            var mqTimer = meterRegistry.find("health.ping.duration").tag("service", "ibm-mq").timer();
            var kafkaTimer = meterRegistry.find("health.ping.duration").tag("service", "kafka").timer();
            var oracleTimer = meterRegistry.find("health.ping.duration").tag("service", "oracle").timer();

            assertThat(mqTimer).isNotNull();
            assertThat(mqTimer.count()).isEqualTo(1);
            assertThat(kafkaTimer).isNotNull();
            assertThat(kafkaTimer.count()).isEqualTo(1);
            assertThat(oracleTimer).isNotNull();
            assertThat(oracleTimer.count()).isEqualTo(1);
        }
    }

    @SuppressWarnings("unchecked")
    private void setupKafkaSuccess(org.mockito.MockedStatic<AdminClient> adminClientMock) throws Exception {
        var adminClient = mock(AdminClient.class);
        var listTopicsResult = mock(ListTopicsResult.class);
        var future = mock(KafkaFuture.class);

        adminClientMock.when(() -> AdminClient.create(any(Map.class))).thenReturn(adminClient);
        lenient().when(adminClient.listTopics()).thenReturn(listTopicsResult);
        lenient().when(listTopicsResult.names()).thenReturn(future);
        lenient().when(future.get(anyLong(), any(TimeUnit.class))).thenReturn(Set.of("topic1"));
    }

    @SuppressWarnings("unchecked")
    private void setupKafkaFailure(org.mockito.MockedStatic<AdminClient> adminClientMock) throws Exception {
        var adminClient = mock(AdminClient.class);
        var listTopicsResult = mock(ListTopicsResult.class);
        var future = mock(KafkaFuture.class);

        adminClientMock.when(() -> AdminClient.create(any(Map.class))).thenReturn(adminClient);
        lenient().when(adminClient.listTopics()).thenReturn(listTopicsResult);
        lenient().when(listTopicsResult.names()).thenReturn(future);
        lenient().when(future.get(anyLong(), any(TimeUnit.class))).thenAnswer(invocation -> {
            throw new TimeoutException("Kafka timeout");
        });
    }

    private void setupOracleSuccess(org.mockito.MockedStatic<DriverManager> driverManagerMock) throws SQLException {
        var oracleConnection = mock(java.sql.Connection.class);
        driverManagerMock.when(() -> DriverManager.getConnection(anyString(), anyString(), anyString()))
                .thenReturn(oracleConnection);
        lenient().when(oracleConnection.isValid(anyInt())).thenReturn(true);
    }

    private void setupOracleFailure(org.mockito.MockedStatic<DriverManager> driverManagerMock) {
        driverManagerMock.when(() -> DriverManager.getConnection(anyString(), anyString(), anyString()))
                .thenAnswer(invocation -> {
                    throw new SQLException("Oracle connection failed");
                });
    }
}
