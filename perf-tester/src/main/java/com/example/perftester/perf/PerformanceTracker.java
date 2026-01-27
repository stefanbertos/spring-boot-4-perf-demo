package com.example.perftester.perf;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Service
public class PerformanceTracker {

    private final Timer e2eLatencyTimer;
    private final ConcurrentHashMap<String, Long> inFlightMessages = new ConcurrentHashMap<>();

    private volatile CountDownLatch completionLatch;
    private volatile long testStartTime;
    private final AtomicLong completedCount = new AtomicLong(0);
    private final AtomicLong totalLatencyNanos = new AtomicLong(0);
    private final AtomicLong minLatencyNanos = new AtomicLong(Long.MAX_VALUE);
    private final AtomicLong maxLatencyNanos = new AtomicLong(0);

    public PerformanceTracker(MeterRegistry meterRegistry) {
        this.e2eLatencyTimer = Timer.builder("mq.e2e.latency")
                .description("End-to-end message processing latency")
                .publishPercentiles(0.5, 0.75, 0.90, 0.95, 0.99)
                .publishPercentileHistogram()
                .register(meterRegistry);
    }

    public void startTest(int messageCount) {
        inFlightMessages.clear();
        completedCount.set(0);
        totalLatencyNanos.set(0);
        minLatencyNanos.set(Long.MAX_VALUE);
        maxLatencyNanos.set(0);
        completionLatch = new CountDownLatch(messageCount);
        testStartTime = System.nanoTime();
        log.info("Started performance test with {} messages", messageCount);
    }

    public void recordSend(String messageId) {
        inFlightMessages.put(messageId, System.nanoTime());
    }

    public void recordReceive(String messageId) {
        Long sendTime = inFlightMessages.remove(messageId);
        if (sendTime != null) {
            long latencyNanos = System.nanoTime() - sendTime;

            // Record to Micrometer timer (automatically pushed to Prometheus)
            e2eLatencyTimer.record(Duration.ofNanos(latencyNanos));

            // Update statistics
            completedCount.incrementAndGet();
            totalLatencyNanos.addAndGet(latencyNanos);
            updateMin(latencyNanos);
            updateMax(latencyNanos);

            if (completionLatch != null) {
                completionLatch.countDown();
            }
        } else {
            log.warn("Received response for unknown message ID: {}", messageId);
        }
    }

    private void updateMin(long value) {
        long current;
        do {
            current = minLatencyNanos.get();
            if (value >= current) {
                return;
            }
        } while (!minLatencyNanos.compareAndSet(current, value));
    }

    private void updateMax(long value) {
        long current;
        do {
            current = maxLatencyNanos.get();
            if (value <= current) {
                return;
            }
        } while (!maxLatencyNanos.compareAndSet(current, value));
    }

    public boolean awaitCompletion(long timeout, TimeUnit unit) throws InterruptedException {
        if (completionLatch == null) {
            return true;
        }
        return completionLatch.await(timeout, unit);
    }

    public PerfTestResult getResult() {
        long completed = completedCount.get();
        long testDurationNanos = System.nanoTime() - testStartTime;
        double testDurationSeconds = testDurationNanos / 1_000_000_000.0;

        double tps = completed / testDurationSeconds;
        double avgLatencyMs = completed > 0
                ? (totalLatencyNanos.get() / completed) / 1_000_000.0
                : 0;
        double minLatencyMs = minLatencyNanos.get() != Long.MAX_VALUE
                ? minLatencyNanos.get() / 1_000_000.0
                : 0;
        double maxLatencyMs = maxLatencyNanos.get() / 1_000_000.0;

        return new PerfTestResult(
                completed,
                inFlightMessages.size(),
                testDurationSeconds,
                tps,
                avgLatencyMs,
                minLatencyMs,
                maxLatencyMs
        );
    }

    public static String extractMessageId(String message) {
        int separatorIndex = message.indexOf('|');
        if (separatorIndex > 0) {
            return message.substring(0, separatorIndex);
        }
        return null;
    }

    public static String createMessage(String messageId, String payload) {
        return messageId + "|" + payload;
    }
}
