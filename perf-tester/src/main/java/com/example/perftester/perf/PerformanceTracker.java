package com.example.perftester.perf;

import com.example.perftester.config.PerfProperties;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Service
public class PerformanceTracker {

    private final long tpsWindowMs;
    private final Timer e2eLatencyTimer;
    private final ConcurrentHashMap<String, Long> inFlightMessages = new ConcurrentHashMap<>();
    private final ConcurrentLinkedDeque<Long> completionTimestamps = new ConcurrentLinkedDeque<>();

    private volatile CountDownLatch completionLatch;
    private volatile long testStartTime;
    private final AtomicLong completedCount = new AtomicLong(0);
    private final AtomicLong totalLatencyNanos = new AtomicLong(0);
    private final AtomicLong minLatencyNanos = new AtomicLong(Long.MAX_VALUE);
    private final AtomicLong maxLatencyNanos = new AtomicLong(Long.MIN_VALUE);

    public PerformanceTracker(MeterRegistry meterRegistry, PerfProperties perfProperties) {
        this.tpsWindowMs = perfProperties.tpsWindowMs();
        this.e2eLatencyTimer = Timer.builder("mq.e2e.latency")
                .description("End-to-end message processing latency")
                .publishPercentiles(0.5, 0.75, 0.90, 0.95, 0.99)
                .publishPercentileHistogram()
                .register(meterRegistry);
    }

    public void startTest(int messageCount) {
        inFlightMessages.clear();
        completionTimestamps.clear();
        completedCount.set(0);
        totalLatencyNanos.set(0);
        minLatencyNanos.set(Long.MAX_VALUE);
        maxLatencyNanos.set(Long.MIN_VALUE);
        completionLatch = new CountDownLatch(messageCount);
        testStartTime = System.nanoTime();
        log.info("Started performance test with {} messages", messageCount);
    }

    public void recordSend(String messageId) {
        inFlightMessages.put(messageId, System.nanoTime());
    }

    public long recordReceive(String messageId) {
        var sendTime = inFlightMessages.remove(messageId);
        if (sendTime != null) {
            long latencyNanos = System.nanoTime() - sendTime;
            long currentTimeMs = System.currentTimeMillis();

            // Record completion timestamp for TPS calculation
            completionTimestamps.addLast(currentTimeMs);

            // Record to Micrometer timer (automatically pushed to Prometheus)
            e2eLatencyTimer.record(Duration.ofNanos(latencyNanos));

            // Update statistics
            completedCount.incrementAndGet();
            totalLatencyNanos.addAndGet(latencyNanos);
            updateMin(latencyNanos);
            updateMax(latencyNanos);

            var latch = completionLatch;
            if (latch != null) {
                latch.countDown();
            }
            return latencyNanos;
        } else {
            log.warn("Received response for unknown message ID: {}", messageId);
            return -1;
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
        var latch = completionLatch;
        if (latch == null) {
            return true;
        }
        return latch.await(timeout, unit);
    }

    public PerfTestResult getResult() {
        var completed = completedCount.get();
        var testDurationNanos = System.nanoTime() - testStartTime;
        var testDurationSeconds = testDurationNanos / 1_000_000_000.0;

        // Calculate TPS over 1-minute window (same as Grafana rate[1m])
        var tps = calculateWindowedTps();

        double avgLatencyMs = completed > 0
                ? (totalLatencyNanos.get() / completed) / 1_000_000.0
                : 0;
        double minLatencyMs = minLatencyNanos.get() != Long.MAX_VALUE
                ? minLatencyNanos.get() / 1_000_000.0
                : 0;
        double maxLatencyMs = maxLatencyNanos.get() != Long.MIN_VALUE
                ? maxLatencyNanos.get() / 1_000_000.0
                : 0;

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

    /**
     * Calculates TPS over a 1-minute sliding window, matching Grafana's rate[1m] calculation.
     * If the test duration is less than 1 minute, calculates rate over the actual duration.
     */
    private double calculateWindowedTps() {
        long currentTimeMs = System.currentTimeMillis();
        long windowStartMs = currentTimeMs - tpsWindowMs;

        // Remove timestamps older than the window
        while (!completionTimestamps.isEmpty()) {
            var oldest = completionTimestamps.peekFirst();
            if (oldest != null && oldest < windowStartMs) {
                completionTimestamps.pollFirst();
            } else {
                break;
            }
        }

        int messagesInWindow = completionTimestamps.size();

        if (messagesInWindow == 0) {
            return 0.0;
        }

        // Find actual time span of messages in window
        var firstTimestamp = completionTimestamps.peekFirst();
        var lastTimestamp = completionTimestamps.peekLast();

        if (firstTimestamp == null || lastTimestamp == null) {
            return 0.0;
        }

        // Calculate the effective window duration
        // Use the smaller of: actual message span or 1 minute
        long actualSpanMs = lastTimestamp - firstTimestamp;
        long effectiveWindowMs = Math.min(Math.max(actualSpanMs, 1), tpsWindowMs);

        // TPS = messages in window / window duration in seconds
        return messagesInWindow / (effectiveWindowMs / 1000.0);
    }

    public static String extractMessageId(String message) {
        var separatorIndex = message.indexOf('|');
        if (separatorIndex > 0) {
            return message.substring(0, separatorIndex);
        }
        return null;
    }

    public static String createMessage(String messageId, String payload) {
        return messageId + "|" + payload;
    }
}
