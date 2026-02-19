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
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Component
@EnableConfigurationProperties(HealthCheckProperties.class)
public class HealthCheckScheduler {

    private final HealthCheckProperties properties;
    private final List<ServiceMetrics> serviceMetrics;

    public HealthCheckScheduler(HealthCheckProperties properties, MeterRegistry meterRegistry) {
        this.properties = properties;

        var serviceConfigs = Map.of(
                "kafka", properties.kafka(),
                "ibm-mq", properties.mq(),
                "postgres", properties.postgres(),
                "redis", properties.redis()
        );

        this.serviceMetrics = serviceConfigs.entrySet().stream()
                .map(entry -> registerServiceMetrics(entry.getKey(), entry.getValue(), meterRegistry))
                .toList();

        log.info("Health check scheduler initialized - Kafka: {}:{}, MQ: {}:{}, Postgres: {}:{}, Redis: {}:{}",
                properties.kafka().host(), properties.kafka().port(),
                properties.mq().host(), properties.mq().port(),
                properties.postgres().host(), properties.postgres().port(),
                properties.redis().host(), properties.redis().port());
    }

    private ServiceMetrics registerServiceMetrics(String serviceName,
                                                   HealthCheckProperties.ServiceEndpoint endpoint,
                                                   MeterRegistry meterRegistry) {
        var status = new AtomicInteger(0);

        Gauge.builder("health.infra.status", status, AtomicInteger::get)
                .description("Infrastructure health status (1=up, 0=down)")
                .tag("service", serviceName)
                .register(meterRegistry);

        var timer = Timer.builder("health.ping.duration")
                .description("Health check ping duration")
                .tag("service", serviceName)
                .register(meterRegistry);

        return new ServiceMetrics(serviceName, endpoint, status, timer);
    }

    @Scheduled(fixedRateString = "${app.healthcheck.interval-ms:60000}")
    public void performHealthChecks() {
        log.debug("Performing infrastructure health checks...");
        for (var metrics : serviceMetrics) {
            checkTcpPort(metrics);
        }
    }

    private void checkTcpPort(ServiceMetrics metrics) {
        var startTime = System.nanoTime();
        try (var socket = new Socket()) {
            socket.connect(new InetSocketAddress(metrics.endpoint().host(), metrics.endpoint().port()),
                    properties.connectionTimeoutMs());
            metrics.status().set(1);
            var duration = System.nanoTime() - startTime;
            metrics.timer().record(duration, TimeUnit.NANOSECONDS);
            log.debug("{} health check passed in {}ms", metrics.name(), TimeUnit.NANOSECONDS.toMillis(duration));
        } catch (IOException e) {
            metrics.status().set(0);
            var duration = System.nanoTime() - startTime;
            metrics.timer().record(duration, TimeUnit.NANOSECONDS);
            log.warn("{} health check failed: {}", metrics.name(), e.getMessage());
        }
    }

    private record ServiceMetrics(String name, HealthCheckProperties.ServiceEndpoint endpoint,
                                   AtomicInteger status, Timer timer) {
    }
}
