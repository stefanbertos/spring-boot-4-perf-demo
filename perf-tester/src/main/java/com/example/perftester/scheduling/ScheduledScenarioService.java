package com.example.perftester.scheduling;

import com.example.perftester.messaging.MessageSender;
import com.example.perftester.perf.PerformanceTracker;
import com.example.perftester.perf.ThresholdEvaluator;
import com.example.perftester.perf.ThresholdResult;
import com.example.perftester.persistence.TestScenarioService;
import com.example.perftester.persistence.TestRunService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScheduledScenarioService {

    private static final DateTimeFormatter HH_MM = DateTimeFormatter.ofPattern("HH:mm");
    private static final int DEFAULT_TIMEOUT_SECONDS = 300;

    private final TestScenarioService testScenarioService;
    private final MessageSender messageSender;
    private final PerformanceTracker performanceTracker;
    private final TestRunService testRunService;
    private final ThresholdEvaluator thresholdEvaluator;

    @Scheduled(cron = "0 * * * * *")
    public void runScheduledScenarios() {
        var currentTime = LocalTime.now().format(HH_MM);
        var snapshot = performanceTracker.getProgressSnapshot();
        if ("RUNNING".equals(snapshot.status()) || "EXPORTING".equals(snapshot.status())) {
            log.debug("Skipping scheduled scenarios at {} — a test is already running", currentTime);
            return;
        }

        var matching = testScenarioService.listScheduledEnabled().stream()
                .filter(s -> currentTime.equals(s.scheduledTime()))
                .toList();

        if (matching.isEmpty()) {
            return;
        }

        for (var scenario : matching) {
            log.info("Triggering scheduled scenario '{}' (id={}) at {}", scenario.name(), scenario.id(), currentTime);
            Thread.ofVirtual()
                    .name("scheduled-test-" + scenario.id() + "-" + currentTime)
                    .start(() -> executeScenario(scenario.id(), scenario.name(), scenario.count()));
        }
    }

    private void executeScenario(Long scenarioId, String scenarioName, int count) {
        var testRunId = UUID.randomUUID().toString();
        var testId = "scheduled-" + scenarioName;
        var scenario = testScenarioService.getById(scenarioId);
        var testRunEntity = testRunService.createRun(testRunId, testId, count, scenario.testType());

        try {
            log.info("Starting scheduled test: scenario='{}', count={}, testRunId={}",
                    scenarioName, count, testRunId);

            int warmupCount = testScenarioService.getWarmupCount(scenarioId);
            if (warmupCount > 0) {
                performanceTracker.startWarmupPhase(warmupCount);
                for (int i = 0; i < warmupCount; i++) {
                    messageSender.sendMessage("warmup-" + i);
                }
                performanceTracker.awaitWarmupCompletion(60, TimeUnit.SECONDS);
                log.info("Warmup complete for scenario '{}'", scenarioName);
            }

            performanceTracker.startTest(count, testRunId);

            var pool = testScenarioService.buildMessagePool(scenarioId);
            var futures = new CompletableFuture<?>[count];
            for (int i = 0; i < count; i++) {
                var msg = pool.isEmpty() ? null : pool.get(i % pool.size());
                var content = msg != null ? msg.content() : "";
                var props = msg != null ? msg.jmsProperties() : Map.<String, String>of();
                futures[i] = props.isEmpty()
                        ? messageSender.sendMessage(content + "-" + i)
                        : messageSender.sendMessage(content + "-" + i, props);
            }
            CompletableFuture.allOf(futures).join();

            var completed = performanceTracker.awaitCompletion(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            var status = completed ? "COMPLETED" : "TIMEOUT";
            var result = performanceTracker.getResult();

            performanceTracker.setStatus(status);
            testRunService.completeRun(testRunEntity.getId(), status, result, null);

            var thresholds = testScenarioService.getScenarioThresholds(scenarioId);
            if (!thresholds.isEmpty()) {
                var thresholdResults = thresholdEvaluator.evaluate(thresholds, result);
                var passed = thresholdResults.stream().allMatch(ThresholdResult::passed);
                testRunService.updateThresholdResult(testRunEntity.getId(),
                        passed ? "PASSED" : "FAILED", thresholdResults);
            }

            log.info("Scheduled test {}: scenario='{}', {}/{} messages, TPS={}, avgLatency={}ms",
                    status.toLowerCase(), scenarioName, result.completedMessages(), count,
                    String.format("%.2f", result.tps()),
                    String.format("%.2f", result.avgLatencyMs()));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            performanceTracker.setStatus("FAILED");
            testRunService.completeRun(testRunEntity.getId(), "FAILED", performanceTracker.getResult(), null);
            log.error("Scheduled test interrupted: scenario='{}', testRunId={}", scenarioName, testRunId);
        } catch (Exception e) {
            performanceTracker.setStatus("FAILED");
            testRunService.completeRun(testRunEntity.getId(), "FAILED", performanceTracker.getResult(), null);
            log.error("Scheduled test failed: scenario='{}', testRunId={}", scenarioName, testRunId, e);
        }
    }
}
