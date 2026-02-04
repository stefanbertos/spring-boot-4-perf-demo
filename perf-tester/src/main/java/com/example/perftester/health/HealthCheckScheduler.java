package com.example.perftester.health;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Component
@EnableConfigurationProperties(HealthCheckProperties.class)
public class HealthCheckScheduler {

    private final HealthCheckProperties properties;

    private final AtomicInteger kafkaStatus = new AtomicInteger(0);
    private final AtomicInteger mqStatus = new AtomicInteger(0);
    private final AtomicInteger oracleStatus = new AtomicInteger(0);
    private final AtomicInteger redisStatus = new AtomicInteger(0);

    private final Timer kafkaPingTimer;
    private final Timer mqPingTimer;
    private final Timer oraclePingTimer;
    private final Timer redisPingTimer;

    public HealthCheckScheduler(HealthCheckProperties properties, MeterRegistry meterRegistry) {
        this.properties = properties;

        Gauge.builder("health.infra.status", kafkaStatus, AtomicInteger::get)
                .description("Infrastructure health status (1=up, 0=down)")
                .tag("service", "kafka")
                .register(meterRegistry);

        Gauge.builder("health.infra.status", mqStatus, AtomicInteger::get)
                .description("Infrastructure health status (1=up, 0=down)")
                .tag("service", "ibm-mq")
                .register(meterRegistry);

        Gauge.builder("health.infra.status", oracleStatus, AtomicInteger::get)
                .description("Infrastructure health status (1=up, 0=down)")
                .tag("service", "oracle")
                .register(meterRegistry);

        Gauge.builder("health.infra.status", redisStatus, AtomicInteger::get)
                .description("Infrastructure health status (1=up, 0=down)")
                .tag("service", "redis")
                .register(meterRegistry);

        this.kafkaPingTimer = Timer.builder("health.ping.duration")
                .description("Health check ping duration")
                .tag("service", "kafka")
                .register(meterRegistry);

        this.mqPingTimer = Timer.builder("health.ping.duration")
                .description("Health check ping duration")
                .tag("service", "ibm-mq")
                .register(meterRegistry);

        this.oraclePingTimer = Timer.builder("health.ping.duration")
                .description("Health check ping duration")
                .tag("service", "oracle")
                .register(meterRegistry);

        this.redisPingTimer = Timer.builder("health.ping.duration")
                .description("Health check ping duration")
                .tag("service", "redis")
                .register(meterRegistry);

        log.info("Health check scheduler initialized - Kafka: {}:{}, MQ: {}:{}, Oracle: {}:{}, Redis: {}:{}",
                properties.kafka().host(), properties.kafka().port(),
                properties.mq().host(), properties.mq().port(),
                properties.oracle().host(), properties.oracle().port(),
                properties.redis().host(), properties.redis().port());
    }

    @Scheduled(fixedRateString = "${app.healthcheck.interval-ms:60000}")
    public void performHealthChecks() {
        log.debug("Performing infrastructure health checks...");
        checkKafka();
        checkMq();
        checkOracle();
        checkRedis();
    }

    private void checkKafka() {
        checkTcpPort("Kafka", properties.kafka(), kafkaStatus, kafkaPingTimer);
    }

    private void checkMq() {
        checkTcpPort("IBM MQ", properties.mq(), mqStatus, mqPingTimer);
    }

    private void checkOracle() {
        checkTcpPort("Oracle", properties.oracle(), oracleStatus, oraclePingTimer);
    }

    private void checkRedis() {
        checkTcpPort("Redis", properties.redis(), redisStatus, redisPingTimer);
    }

    private void checkTcpPort(String serviceName, HealthCheckProperties.ServiceEndpoint endpoint,
                              AtomicInteger status, Timer timer) {
        var startTime = System.nanoTime();
        try (var socket = new Socket()) {
            socket.connect(new InetSocketAddress(endpoint.host(), endpoint.port()),
                    properties.connectionTimeoutMs());
            status.set(1);
            var duration = System.nanoTime() - startTime;
            timer.record(duration, TimeUnit.NANOSECONDS);
            log.debug("{} health check passed in {}ms", serviceName, TimeUnit.NANOSECONDS.toMillis(duration));
        } catch (IOException e) {
            status.set(0);
            var duration = System.nanoTime() - startTime;
            timer.record(duration, TimeUnit.NANOSECONDS);
            log.warn("{} health check failed: {}", serviceName, e.getMessage());
        }
    }
}
