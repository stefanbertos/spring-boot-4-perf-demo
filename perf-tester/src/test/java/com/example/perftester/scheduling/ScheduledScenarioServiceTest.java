package com.example.perftester.scheduling;

import com.example.perftester.messaging.MessageSender;
import com.example.perftester.perf.PerfTestResult;
import com.example.perftester.perf.PerformanceTracker;
import com.example.perftester.perf.ThresholdDef;
import com.example.perftester.perf.ThresholdEvaluator;
import com.example.perftester.perf.ThresholdResult;
import com.example.perftester.persistence.ScenarioMessage;
import com.example.perftester.persistence.TestRun;
import com.example.perftester.persistence.TestRunService;
import com.example.perftester.persistence.TestScenarioDetail;
import com.example.perftester.persistence.TestScenarioService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static java.time.Duration.ofSeconds;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ScheduledScenarioServiceTest {

    private static final DateTimeFormatter HH_MM = DateTimeFormatter.ofPattern("HH:mm");

    @Mock
    private TestScenarioService testScenarioService;

    @Mock
    private MessageSender messageSender;

    @Mock
    private PerformanceTracker performanceTracker;

    @Mock
    private TestRunService testRunService;

    @Mock
    private ThresholdEvaluator thresholdEvaluator;

    @InjectMocks
    private ScheduledScenarioService scheduledScenarioService;

    private TestScenarioDetail scenarioAt(String scheduledTime) {
        return new TestScenarioDetail(
                1L, "test-scenario", 2, List.of(), true, scheduledTime,
                0, null, null, null, List.of(), "2024-01-01T00:00:00Z", "2024-01-01T00:00:00Z");
    }

    private TestRun mockTestRun(Long id) {
        var run = new TestRun();
        run.setId(id);
        return run;
    }

    private PerfTestResult emptyResult() {
        return new PerfTestResult(0, 0, 0, 0, 0, 0, 0);
    }

    /** Stubs the tracker so that the active-check passes and tryStart() succeeds. */
    private void trackerIdle() {
        when(performanceTracker.isActive()).thenReturn(false);
        when(performanceTracker.tryStart(anyInt(), anyString())).thenReturn(true);
    }

    @Test
    void shouldSkipWhenTestIsRunning() {
        when(performanceTracker.isActive()).thenReturn(true);

        scheduledScenarioService.runScheduledScenarios();

        verify(testScenarioService, never()).listScheduledEnabled();
    }

    @Test
    void shouldSkipWhenTestIsExporting() {
        when(performanceTracker.isActive()).thenReturn(true);

        scheduledScenarioService.runScheduledScenarios();

        verify(testScenarioService, never()).listScheduledEnabled();
    }

    @Test
    void shouldDoNothingWhenNoScenariosScheduled() {
        when(performanceTracker.isActive()).thenReturn(false);
        when(testScenarioService.listScheduledEnabled()).thenReturn(List.of());

        scheduledScenarioService.runScheduledScenarios();

        verify(messageSender, never()).sendMessage(anyString());
    }

    @Test
    void shouldDoNothingWhenNoScenariosMatchCurrentTime() {
        when(performanceTracker.isActive()).thenReturn(false);
        when(testScenarioService.listScheduledEnabled()).thenReturn(List.of(scenarioAt("99:99")));

        scheduledScenarioService.runScheduledScenarios();

        verify(messageSender, never()).sendMessage(anyString());
    }

    @Test
    void shouldTriggerExecutionForMatchingScenario() throws Exception {
        var currentTime = LocalTime.now().format(HH_MM);
        trackerIdle();
        when(testScenarioService.listScheduledEnabled()).thenReturn(List.of(scenarioAt(currentTime)));
        when(testScenarioService.getById(1L)).thenReturn(scenarioAt(currentTime));
        when(testRunService.createRun(anyString(), anyString(), anyInt(), any())).thenReturn(mockTestRun(10L));
        when(testScenarioService.buildMessagePool(1L)).thenReturn(List.of());
        when(testScenarioService.getScenarioThresholds(1L)).thenReturn(List.of());
        when(messageSender.sendMessage(anyString())).thenReturn(CompletableFuture.completedFuture(null));
        when(performanceTracker.awaitCompletion(anyLong(), any(TimeUnit.class))).thenReturn(true);
        when(performanceTracker.getResult()).thenReturn(emptyResult());

        scheduledScenarioService.runScheduledScenarios();

        await().atMost(ofSeconds(5)).untilAsserted(() -> {
            verify(performanceTracker).tryStart(eq(2), anyString());
            verify(testRunService).completeRun(eq(10L), eq("COMPLETED"), any(), eq(null));
        });
    }

    @Test
    void shouldRecordTimeoutStatusWhenAwaitCompletionReturnsFalse() throws Exception {
        var currentTime = LocalTime.now().format(HH_MM);
        trackerIdle();
        when(testScenarioService.listScheduledEnabled()).thenReturn(List.of(scenarioAt(currentTime)));
        when(testScenarioService.getById(1L)).thenReturn(scenarioAt(currentTime));
        when(testRunService.createRun(anyString(), anyString(), anyInt(), any())).thenReturn(mockTestRun(20L));
        when(testScenarioService.buildMessagePool(1L)).thenReturn(List.of());
        when(messageSender.sendMessage(anyString())).thenReturn(CompletableFuture.completedFuture(null));
        when(performanceTracker.awaitCompletion(anyLong(), any(TimeUnit.class))).thenReturn(false);
        when(testScenarioService.getScenarioThresholds(1L)).thenReturn(List.of());
        when(performanceTracker.getResult()).thenReturn(emptyResult());

        scheduledScenarioService.runScheduledScenarios();

        await().atMost(ofSeconds(5)).untilAsserted(() ->
                verify(testRunService).completeRun(eq(20L), eq("TIMEOUT"), any(), eq(null)));
    }

    @Test
    void shouldHandleExceptionDuringExecution() {
        var currentTime = LocalTime.now().format(HH_MM);
        trackerIdle();
        when(testScenarioService.listScheduledEnabled()).thenReturn(List.of(scenarioAt(currentTime)));
        when(testScenarioService.getById(1L)).thenReturn(scenarioAt(currentTime));
        when(testRunService.createRun(anyString(), anyString(), anyInt(), any())).thenReturn(mockTestRun(30L));
        when(testScenarioService.buildMessagePool(1L)).thenThrow(new RuntimeException("DB error"));
        when(performanceTracker.getResult()).thenReturn(emptyResult());

        scheduledScenarioService.runScheduledScenarios();

        await().atMost(ofSeconds(5)).untilAsserted(() -> {
            verify(performanceTracker).setStatus("FAILED");
            verify(testRunService).completeRun(eq(30L), eq("FAILED"), any(), eq(null));
        });
    }

    @Test
    void shouldHandleInterruptedExceptionDuringExecution() throws Exception {
        var currentTime = LocalTime.now().format(HH_MM);
        trackerIdle();
        when(testScenarioService.listScheduledEnabled()).thenReturn(List.of(scenarioAt(currentTime)));
        when(testScenarioService.getById(1L)).thenReturn(scenarioAt(currentTime));
        when(testRunService.createRun(anyString(), anyString(), anyInt(), any())).thenReturn(mockTestRun(40L));
        when(testScenarioService.buildMessagePool(1L)).thenReturn(List.of());
        when(messageSender.sendMessage(anyString())).thenReturn(CompletableFuture.completedFuture(null));
        doThrow(new InterruptedException()).when(performanceTracker).awaitCompletion(anyLong(), any(TimeUnit.class));
        when(performanceTracker.getResult()).thenReturn(emptyResult());

        scheduledScenarioService.runScheduledScenarios();

        await().atMost(ofSeconds(5)).untilAsserted(() -> {
            verify(performanceTracker).setStatus("FAILED");
            verify(testRunService).completeRun(eq(40L), eq("FAILED"), any(), eq(null));
        });
    }

    @Test
    void shouldRecordFailedStatusWhenTrackerRejectsStart() {
        var currentTime = LocalTime.now().format(HH_MM);
        when(performanceTracker.isActive()).thenReturn(false);
        when(performanceTracker.tryStart(anyInt(), anyString())).thenReturn(false);
        when(testScenarioService.listScheduledEnabled()).thenReturn(List.of(scenarioAt(currentTime)));
        when(testScenarioService.getById(1L)).thenReturn(scenarioAt(currentTime));
        when(testRunService.createRun(anyString(), anyString(), anyInt(), any())).thenReturn(mockTestRun(80L));
        when(performanceTracker.getResult()).thenReturn(emptyResult());

        scheduledScenarioService.runScheduledScenarios();

        await().atMost(ofSeconds(5)).untilAsserted(() ->
                verify(testRunService).completeRun(eq(80L), eq("FAILED"), any(), eq(null)));
    }

    @Test
    void shouldExecuteWarmupBeforeMainTest() throws Exception {
        var currentTime = LocalTime.now().format(HH_MM);
        trackerIdle();
        when(testScenarioService.listScheduledEnabled()).thenReturn(List.of(scenarioAt(currentTime)));
        when(testScenarioService.getById(1L)).thenReturn(scenarioAt(currentTime));
        when(testRunService.createRun(anyString(), anyString(), anyInt(), any())).thenReturn(mockTestRun(50L));
        when(testScenarioService.getWarmupCount(1L)).thenReturn(3);
        when(testScenarioService.buildMessagePool(1L)).thenReturn(List.of());
        when(messageSender.sendMessage(anyString())).thenReturn(CompletableFuture.completedFuture(null));
        when(performanceTracker.awaitWarmupCompletion(anyLong(), any(TimeUnit.class))).thenReturn(true);
        when(performanceTracker.awaitCompletion(anyLong(), any(TimeUnit.class))).thenReturn(true);
        when(testScenarioService.getScenarioThresholds(1L)).thenReturn(List.of());
        when(performanceTracker.getResult()).thenReturn(emptyResult());

        scheduledScenarioService.runScheduledScenarios();

        await().atMost(ofSeconds(5)).untilAsserted(() ->
                verify(performanceTracker).startWarmupPhase(3));
    }

    @Test
    void shouldUseMessagePoolWhenNonEmpty() throws Exception {
        var currentTime = LocalTime.now().format(HH_MM);
        var poolMsg = new ScenarioMessage("payload", Map.of(), null);
        trackerIdle();
        when(testScenarioService.listScheduledEnabled()).thenReturn(List.of(scenarioAt(currentTime)));
        when(testScenarioService.getById(1L)).thenReturn(scenarioAt(currentTime));
        when(testRunService.createRun(anyString(), anyString(), anyInt(), any())).thenReturn(mockTestRun(60L));
        when(testScenarioService.buildMessagePool(1L)).thenReturn(List.of(poolMsg, poolMsg));
        when(messageSender.sendMessage(anyString()))
                .thenReturn(CompletableFuture.completedFuture(null));
        when(performanceTracker.awaitCompletion(anyLong(), any(TimeUnit.class))).thenReturn(true);
        when(testScenarioService.getScenarioThresholds(1L)).thenReturn(List.of());
        when(performanceTracker.getResult()).thenReturn(emptyResult());

        scheduledScenarioService.runScheduledScenarios();

        await().atMost(ofSeconds(5)).untilAsserted(() ->
                verify(messageSender, atLeastOnce()).sendMessage(anyString()));
    }

    @Test
    void shouldEvaluateThresholdsWhenNonEmpty() throws Exception {
        var currentTime = LocalTime.now().format(HH_MM);
        var threshold = new ThresholdDef("TPS", "GTE", 5.0);
        var thresholdResult = new ThresholdResult("TPS", "GTE", 5.0, 10.0, true);
        trackerIdle();
        when(testScenarioService.listScheduledEnabled()).thenReturn(List.of(scenarioAt(currentTime)));
        when(testScenarioService.getById(1L)).thenReturn(scenarioAt(currentTime));
        when(testRunService.createRun(anyString(), anyString(), anyInt(), any())).thenReturn(mockTestRun(70L));
        when(testScenarioService.buildMessagePool(1L)).thenReturn(List.of());
        when(messageSender.sendMessage(anyString())).thenReturn(CompletableFuture.completedFuture(null));
        when(performanceTracker.awaitCompletion(anyLong(), any(TimeUnit.class))).thenReturn(true);
        when(testScenarioService.getScenarioThresholds(1L)).thenReturn(List.of(threshold));
        when(thresholdEvaluator.evaluate(any(), any())).thenReturn(List.of(thresholdResult));
        when(performanceTracker.getResult()).thenReturn(emptyResult());

        scheduledScenarioService.runScheduledScenarios();

        await().atMost(ofSeconds(5)).untilAsserted(() ->
                verify(testRunService).updateThresholdResult(anyLong(), anyString(), any()));
    }
}
