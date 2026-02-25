package com.example.perftester.health;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Component
@RequiredArgsConstructor
public class HealthCheckScheduler {

    private final HealthCheckConfigRepository repository;
    private final MeterRegistry meterRegistry;

    private final Map<String, AtomicInteger> statusGauges = new ConcurrentHashMap<>();
    private final Map<String, Timer> timers = new ConcurrentHashMap<>();
    private final Map<String, Long> lastCheckedAt = new ConcurrentHashMap<>();

    @Scheduled(fixedDelay = 5000)
    public void performHealthChecks() {
        var configs = repository.findAll();
        var now = System.currentTimeMillis();
        for (var config : configs) {
            if (!config.isEnabled()) {
                continue;
            }
            var lastChecked = lastCheckedAt.getOrDefault(config.getService(), 0L);
            if (now - lastChecked >= config.getIntervalMs()) {
                checkService(config);
                lastCheckedAt.put(config.getService(), now);
            }
        }
    }

    private void checkService(HealthCheckConfig config) {
        var status = statusGauges.computeIfAbsent(config.getService(), service -> {
            var gauge = new AtomicInteger(0);
            Gauge.builder("health.infra.status", gauge, AtomicInteger::get)
                    .description("Infrastructure health status (1=up, 0=down)")
                    .tag("service", service)
                    .register(meterRegistry);
            return gauge;
        });
        var timer = timers.computeIfAbsent(config.getService(), service ->
                Timer.builder("health.ping.duration")
                        .description("Health check ping duration")
                        .tag("service", service)
                        .register(meterRegistry));
        checkTcpPort(config, status, timer);
    }

    private void checkTcpPort(HealthCheckConfig config, AtomicInteger status, Timer timer) {
        var startTime = System.nanoTime();
        try (var socket = new Socket()) {
            socket.connect(new InetSocketAddress(config.getHost(), config.getPort()),
                    config.getConnectionTimeoutMs());
            status.set(1);
            var duration = System.nanoTime() - startTime;
            timer.record(duration, TimeUnit.NANOSECONDS);
            log.debug("{} health check passed in {}ms", config.getService(),
                    TimeUnit.NANOSECONDS.toMillis(duration));
        } catch (IOException e) {
            status.set(0);
            var duration = System.nanoTime() - startTime;
            timer.record(duration, TimeUnit.NANOSECONDS);
            log.warn("{} health check failed: {}", config.getService(), e.getMessage());
        }
    }
}
