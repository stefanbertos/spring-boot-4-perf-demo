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
        if (performanceTracker.isActive()) {
            log.debug("Skipping scheduled scenarios at {} — a test is already active", currentTime);
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
        var testStarted = false;

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

            if (!performanceTracker.tryStart(count, testRunId)) {
                log.warn("Scheduled test rejected — tracker already active: scenario='{}', testRunId={}",
                        scenarioName, testRunId);
                testRunService.completeRun(testRunEntity.getId(), "FAILED", performanceTracker.getResult(), null);
                return;
            }
            testStarted = true;

            var pool = testScenarioService.buildMessagePool(scenarioId);
            var futures = new CompletableFuture<?>[count];
            for (int i = 0; i < count; i++) {
                if (!pool.isEmpty()) {
                    var msg = pool.get(i % pool.size());
                    futures[i] = messageSender.sendMessage(msg.content());
                } else {
                    futures[i] = messageSender.sendMessage("msg-" + i);
                }
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
        } finally {
            if (testStarted) {
                performanceTracker.markIdle();
            }
        }
    }
}
