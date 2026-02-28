package com.example.perftester.health;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HealthCheckSchedulerTest {

    @Mock
    private HealthCheckConfigRepository repository;

    private MeterRegistry meterRegistry;
    private ServerSocket serverA;
    private ServerSocket serverB;

    @BeforeEach
    void setUp() throws IOException {
        meterRegistry = new SimpleMeterRegistry();
        serverA = new ServerSocket(0);
        serverB = new ServerSocket(0);
    }

    @AfterEach
    void tearDown() {
        closeQuietly(serverA);
        closeQuietly(serverB);
    }

    private void closeQuietly(ServerSocket s) {
        if (s != null && !s.isClosed()) {
            try {
                s.close();
            } catch (IOException e) {
                // ignore
            }
        }
    }

    private HealthCheckConfig config(String service, String host, int port, boolean enabled) {
        var cfg = new HealthCheckConfig();
        cfg.setService(service);
        cfg.setHost(host);
        cfg.setPort(port);
        cfg.setEnabled(enabled);
        cfg.setConnectionTimeoutMs(1000);
        cfg.setIntervalMs(0);
        return cfg;
    }

    @Test
    void performHealthChecksShouldSetGaugeToOneWhenPortIsOpen() {
        var cfg = config("kafka", "localhost", serverA.getLocalPort(), true);
        when(repository.findAll()).thenReturn(List.of(cfg));

        var scheduler = new HealthCheckScheduler(repository, meterRegistry);
        scheduler.performHealthChecks();

        var gauge = meterRegistry.find("health.infra.status").tag("service", "kafka").gauge();
        assertThat(gauge).isNotNull();
        assertThat(gauge.value()).isEqualTo(1.0);
    }

    @Test
    void performHealthChecksShouldSetGaugeToZeroWhenPortIsClosed() throws IOException {
        serverA.close();
        var cfg = config("kafka", "localhost", serverA.getLocalPort(), true);
        when(repository.findAll()).thenReturn(List.of(cfg));

        var scheduler = new HealthCheckScheduler(repository, meterRegistry);
        scheduler.performHealthChecks();

        var gauge = meterRegistry.find("health.infra.status").tag("service", "kafka").gauge();
        assertThat(gauge).isNotNull();
        assertThat(gauge.value()).isEqualTo(0.0);
    }

    @Test
    void performHealthChecksShouldSkipDisabledConfigs() {
        var cfg = config("kafka", "localhost", serverA.getLocalPort(), false);
        when(repository.findAll()).thenReturn(List.of(cfg));

        var scheduler = new HealthCheckScheduler(repository, meterRegistry);
        scheduler.performHealthChecks();

        var gauge = meterRegistry.find("health.infra.status").tag("service", "kafka").gauge();
        assertThat(gauge).isNull();
    }

    @Test
    void performHealthChecksShouldRegisterTimerOnSuccess() {
        var cfg = config("mq", "localhost", serverB.getLocalPort(), true);
        when(repository.findAll()).thenReturn(List.of(cfg));

        var scheduler = new HealthCheckScheduler(repository, meterRegistry);
        scheduler.performHealthChecks();

        var timer = meterRegistry.find("health.ping.duration").tag("service", "mq").timer();
        assertThat(timer).isNotNull();
        assertThat(timer.count()).isEqualTo(1);
    }

    @Test
    void performHealthChecksShouldRegisterTimerOnFailure() throws IOException {
        serverB.close();
        var cfg = config("mq", "localhost", serverB.getLocalPort(), true);
        cfg.setConnectionTimeoutMs(500);
        when(repository.findAll()).thenReturn(List.of(cfg));

        var scheduler = new HealthCheckScheduler(repository, meterRegistry);
        scheduler.performHealthChecks();

        var timer = meterRegistry.find("health.ping.duration").tag("service", "mq").timer();
        assertThat(timer).isNotNull();
        assertThat(timer.count()).isEqualTo(1);
    }

    @Test
    void performHealthChecksShouldHandleMultipleServices() {
        var cfgA = config("kafka", "localhost", serverA.getLocalPort(), true);
        var cfgB = config("mq", "localhost", serverB.getLocalPort(), true);
        when(repository.findAll()).thenReturn(List.of(cfgA, cfgB));

        var scheduler = new HealthCheckScheduler(repository, meterRegistry);
        scheduler.performHealthChecks();

        assertThat(meterRegistry.find("health.infra.status").tag("service", "kafka").gauge()).isNotNull();
        assertThat(meterRegistry.find("health.infra.status").tag("service", "mq").gauge()).isNotNull();
    }

    @Test
    void resetServiceTimerShouldRemoveCachedTime() {
        var cfg = config("kafka", "localhost", serverA.getLocalPort(), true);
        when(repository.findAll()).thenReturn(List.of(cfg));

        var scheduler = new HealthCheckScheduler(repository, meterRegistry);
        // First run populates lastCheckedAt for "kafka"
        scheduler.performHealthChecks();

        // Reset clears the cached time — covers lines 31-33
        scheduler.resetServiceTimer("kafka");

        // Second run executes checkService again (re-registers timer)
        scheduler.performHealthChecks();

        var timer = meterRegistry.find("health.ping.duration").tag("service", "kafka").timer();
        assertThat(timer).isNotNull();
        assertThat(timer.count()).isEqualTo(2);
    }
}
