package com.example.perftester.persistence.service;

import com.example.perftester.perf.PerfTestResult;
import com.example.perftester.persistence.entity.PerfMessage;
import com.example.perftester.persistence.entity.PerfMetric;
import com.example.perftester.persistence.entity.PerfMetric.MetricType;
import com.example.perftester.persistence.entity.TestRun;
import com.example.perftester.persistence.entity.TestRun.TestStatus;
import com.example.perftester.persistence.repository.PerfMessageRepository;
import com.example.perftester.persistence.repository.PerfMetricRepository;
import com.example.perftester.persistence.repository.TestRunRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class TestResultPersistenceService {

    private final TestRunRepository testRunRepository;
    private final PerfMessageRepository perfMessageRepository;
    private final PerfMetricRepository perfMetricRepository;

    @Transactional
    public TestRun startTestRun(String testId, int messageCount) {
        var testRun = new TestRun(testId, messageCount);
        testRun = testRunRepository.save(testRun);
        log.info("Started test run {} with ID {} for {} messages", testId, testRun.getId(), messageCount);
        return testRun;
    }

    @Transactional
    public void recordMessageSent(TestRun testRun, String messageId, String correlationId,
                                  String traceId, int payloadSize) {
        var message = new PerfMessage(testRun, messageId, payloadSize);
        message.setCorrelationId(correlationId);
        message.setTraceId(traceId);
        perfMessageRepository.save(message);
    }

    @Transactional
    public void recordMessageReceived(String messageId, BigDecimal latencyMs) {
        perfMessageRepository.findByMessageId(messageId).ifPresent(message -> {
            message.markReceived(latencyMs);
            perfMessageRepository.save(message);
        });
    }

    @Transactional
    public void recordMessageFailed(String messageId, String errorMessage) {
        perfMessageRepository.findByMessageId(messageId).ifPresent(message -> {
            message.markFailed(errorMessage);
            perfMessageRepository.save(message);
        });
    }

    @Transactional
    public TestRun completeTestRun(Long testRunId, PerfTestResult result, boolean timedOut) {
        return testRunRepository.findById(testRunId).map(testRun -> {
            testRun.setEndTime(Instant.now());
            testRun.setStatus(timedOut ? TestStatus.TIMEOUT : TestStatus.COMPLETED);
            testRun.setCompletedCount((int) result.completedMessages());
            testRun.setFailedCount((int) result.pendingMessages());
            testRun.setDurationMs((long) (result.testDurationSeconds() * 1000));
            testRun.setTps(BigDecimal.valueOf(result.tps()));
            testRun.setAvgLatencyMs(BigDecimal.valueOf(result.avgLatencyMs()));
            testRun.setMinLatencyMs(BigDecimal.valueOf(result.minLatencyMs()));
            testRun.setMaxLatencyMs(BigDecimal.valueOf(result.maxLatencyMs()));

            if (timedOut) {
                perfMessageRepository.markPendingMessagesAsTimeout(testRunId);
            }

            testRun = testRunRepository.save(testRun);
            log.info("Completed test run {} with status {}", testRunId, testRun.getStatus());
            return testRun;
        }).orElseThrow(() -> new IllegalArgumentException("Test run not found: " + testRunId));
    }

    @Transactional
    public void recordMetric(TestRun testRun, String metricName, MetricType metricType,
                            BigDecimal value, String unit, String tags) {
        var metric = new PerfMetric(testRun, metricName, metricType, value, unit);
        metric.setTags(tags);
        perfMetricRepository.save(metric);
    }

    @Transactional(readOnly = true)
    public Optional<TestRun> findTestRun(Long id) {
        return testRunRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public Optional<TestRun> findTestRunByTestId(String testId) {
        return testRunRepository.findByTestId(testId);
    }

    @Transactional(readOnly = true)
    public List<TestRun> findRecentTestRuns() {
        return testRunRepository.findRecentTestRuns();
    }

    @Transactional(readOnly = true)
    public List<PerfMessage> findMessagesForTestRun(Long testRunId) {
        return perfMessageRepository.findByTestRunId(testRunId);
    }

    @Transactional(readOnly = true)
    public List<PerfMetric> findMetricsForTestRun(Long testRunId) {
        return perfMetricRepository.findByTestRunId(testRunId);
    }

    @Transactional(readOnly = true)
    public TestRunSummary getTestRunSummary(Long testRunId) {
        var testRun = testRunRepository.findById(testRunId)
                .orElseThrow(() -> new IllegalArgumentException("Test run not found: " + testRunId));

        long sentCount = perfMessageRepository.countByTestRunIdAndStatus(testRunId, PerfMessage.MessageStatus.SENT);
        long receivedCount = perfMessageRepository.countByTestRunIdAndStatus(testRunId, PerfMessage.MessageStatus.RECEIVED);
        long failedCount = perfMessageRepository.countByTestRunIdAndStatus(testRunId, PerfMessage.MessageStatus.FAILED);
        long timeoutCount = perfMessageRepository.countByTestRunIdAndStatus(testRunId, PerfMessage.MessageStatus.TIMEOUT);

        var avgLatency = perfMessageRepository.calculateAverageLatency(testRunId);

        return new TestRunSummary(
                testRun.getId(),
                testRun.getTestId(),
                testRun.getStatus(),
                testRun.getMessageCount(),
                sentCount,
                receivedCount,
                failedCount,
                timeoutCount,
                testRun.getStartTime(),
                testRun.getEndTime(),
                testRun.getDurationMs() != null ? Duration.ofMillis(testRun.getDurationMs()) : null,
                testRun.getTps(),
                avgLatency != null ? BigDecimal.valueOf(avgLatency) : null
        );
    }

    public record TestRunSummary(
            Long id,
            String testId,
            TestStatus status,
            int totalMessages,
            long sentCount,
            long receivedCount,
            long failedCount,
            long timeoutCount,
            Instant startTime,
            Instant endTime,
            Duration duration,
            BigDecimal tps,
            BigDecimal avgLatencyMs
    ) {}
}
