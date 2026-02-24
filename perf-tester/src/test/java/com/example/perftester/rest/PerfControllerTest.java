package com.example.perftester.rest;

import com.example.perftester.admin.LoggingAdminService;
import com.example.perftester.config.PerfProperties;
import com.example.perftester.export.TestResultPackager;
import com.example.perftester.export.TestResultPackager.PackageResult;
import com.example.perftester.grafana.GrafanaExportService;
import com.example.perftester.grafana.GrafanaExportService.DashboardExportResult;
import com.example.perftester.kubernetes.KubernetesService;
import com.example.perftester.loki.LogEntry;
import com.example.perftester.loki.LokiService;
import com.example.perftester.messaging.MessageSender;
import com.example.perftester.monitoring.InfraSnapshotService;
import com.example.perftester.perf.PerfTestResult;
import com.example.perftester.perf.PerformanceTracker;
import com.example.perftester.perf.TestProgressEvent;
import com.example.perftester.perf.TestStartResponse;
import com.example.perftester.perf.ThinkTimeCalculator;
import com.example.perftester.perf.ThresholdDef;
import com.example.perftester.perf.ThresholdEvaluator;
import com.example.perftester.perf.ThresholdResult;
import com.example.perftester.persistence.TestRun;
import com.example.perftester.persistence.TestRunService;
import com.example.perftester.persistence.TestScenarioService;
import com.example.perftester.prometheus.PrometheusExportService;
import com.example.perftester.prometheus.PrometheusExportService.PrometheusExportResult;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.boot.logging.LogLevel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PerfControllerTest {

    @Mock
    private MessageSender messageSender;

    @Mock
    private PerformanceTracker performanceTracker;

    @Mock
    private GrafanaExportService grafanaExportService;

    @Mock
    private PrometheusExportService prometheusExportService;

    @Mock
    private TestResultPackager testResultPackager;

    @Mock
    private KubernetesService kubernetesService;

    @Mock
    private LokiService lokiService;

    @Mock
    private LoggingAdminService loggingAdminService;

    @Mock
    private TestRunService testRunService;

    @Mock
    private TestScenarioService testScenarioService;

    @Mock
    private ThinkTimeCalculator thinkTimeCalculator;

    @Mock
    private ThresholdEvaluator thresholdEvaluator;

    @Mock
    private InfraSnapshotService infraSnapshotService;

    @TempDir
    Path tempDir;

    // Use 100ms propagation delay so export tests don't take 16 seconds
    private final PerfProperties perfProperties = new PerfProperties(100, 60000, 60000, 30000, 60, 15);

    private PerfController controller;

    @BeforeEach
    void setUp() throws Exception {
        controller = new PerfController(messageSender, performanceTracker,
                grafanaExportService, prometheusExportService, testResultPackager,
                kubernetesService, lokiService, loggingAdminService, perfProperties, testRunService,
                testScenarioService, thinkTimeCalculator, thresholdEvaluator, infraSnapshotService);

        when(messageSender.sendMessage(anyString())).thenReturn(CompletableFuture.completedFuture(null));
        doReturn(true).when(performanceTracker).awaitCompletion(anyLong(), any(TimeUnit.class));
        var perfResult = new PerfTestResult(10, 0, 1.0, 10.0, 50.0, 10.0, 100.0);
        when(performanceTracker.getResult()).thenReturn(perfResult);
        var mockTestRun = new TestRun();
        mockTestRun.setId(1L);
        when(testRunService.createRun(anyString(), any(), anyInt(), any())).thenReturn(mockTestRun);
        when(performanceTracker.getProgressSnapshot()).thenReturn(
                new TestProgressEvent("run-id", "RUNNING", 5, 3, 10, 30.0, 3.0, 50.0, 10.0, 100.0, 0.5));
        when(kubernetesService.exportClusterInfo()).thenReturn(null);
    }

    @Test
    void sendMessagesShouldReturnAcceptedWithTestRunId() {
        ResponseEntity<TestStartResponse> response = controller.sendMessages(
                "test message", 10, 1, 0, new PerfController.ExportOptions(), new PerfController.RunOptions());

        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().testRunId());
    }

    @Test
    void sendMessagesShouldStartTestAsynchronously() {
        controller.sendMessages("test message", 10, 1, 0, new PerfController.ExportOptions(), new PerfController.RunOptions());

        Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .pollInterval(Duration.ofMillis(100))
                .untilAsserted(() -> verify(messageSender, times(10)).sendMessage(anyString()));
    }

    @Test
    void sendMessagesShouldCallStartTestWithGeneratedRunId() {
        controller.sendMessages("test message", 5, 1, 0, new PerfController.ExportOptions(), new PerfController.RunOptions());

        Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .pollInterval(Duration.ofMillis(100))
                .untilAsserted(() -> verify(performanceTracker).startTest(anyInt(), anyString()));
    }

    @Test
    void sendMessagesShouldSetStatusCompletedOnSuccess() {
        controller.sendMessages("test message", 3, 1, 0, new PerfController.ExportOptions(), new PerfController.RunOptions());

        Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .pollInterval(Duration.ofMillis(100))
                .untilAsserted(() -> verify(performanceTracker).setStatus("COMPLETED"));
    }

    @Test
    void sendMessagesShouldSetStatusTimeoutWhenNotAllReceived() throws Exception {
        doReturn(false).when(performanceTracker).awaitCompletion(anyLong(), any(TimeUnit.class));

        controller.sendMessages("test message", 3, 1, 0, new PerfController.ExportOptions(), new PerfController.RunOptions());

        Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .pollInterval(Duration.ofMillis(100))
                .untilAsserted(() -> verify(performanceTracker).setStatus("TIMEOUT"));
    }

    @Test
    void sendMessagesShouldExportArtifactsWhenExportStatisticsEnabled() throws IOException {
        var dashboardExport = new DashboardExportResult(List.of(), List.of());
        when(grafanaExportService.exportDashboards(anyLong(), anyLong())).thenReturn(dashboardExport);

        var prometheusExport = new PrometheusExportResult(null, null, "No connection");
        when(prometheusExportService.exportMetrics(anyLong(), anyLong(), any())).thenReturn(prometheusExport);

        Path tempZip = tempDir.resolve("test.zip");
        Files.write(tempZip, "content".getBytes());
        when(testResultPackager.packageResults(any(), anyList(), any(), anyList(), any(), anyLong(), anyLong()))
                .thenReturn(new PackageResult("test.zip", tempZip.toString()));

        controller.sendMessages("test message", 3, 1, 0, new PerfController.ExportOptions(true, false, false, false),
                new PerfController.RunOptions("test-id", false, null));

        Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .pollInterval(Duration.ofMillis(100))
                .untilAsserted(() -> verify(grafanaExportService).exportDashboards(anyLong(), anyLong()));
    }

    @Test
    void sendMessagesShouldCleanupExportedFilesAfterExport() throws IOException {
        var dashFile = tempDir.resolve("dashboard.png");
        Files.write(dashFile, "image".getBytes());
        var dashboardExport = new DashboardExportResult(
                List.of("http://grafana/d/1"), List.of(dashFile.toString()));
        when(grafanaExportService.exportDashboards(anyLong(), anyLong())).thenReturn(dashboardExport);

        var promFile = tempDir.resolve("prometheus.json");
        Files.writeString(promFile, "{}");
        var prometheusExport = new PrometheusExportResult(
                promFile.toString(), "http://prometheus/query", null);
        when(prometheusExportService.exportMetrics(anyLong(), anyLong(), any())).thenReturn(prometheusExport);

        Path tempZip = tempDir.resolve("test.zip");
        Files.write(tempZip, "content".getBytes());
        when(testResultPackager.packageResults(any(), anyList(), anyString(), anyList(), any(), anyLong(), anyLong()))
                .thenReturn(new PackageResult("test.zip", tempZip.toString()));

        controller.sendMessages("test message", 1, 1, 0, new PerfController.ExportOptions(true, true, false, false),
                new PerfController.RunOptions("test-id", false, null));

        Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .pollInterval(Duration.ofMillis(100))
                .untilAsserted(() -> {
                    assertFalse(Files.exists(dashFile));
                    assertFalse(Files.exists(promFile));
                });
    }

    @Test
    void sendMessagesShouldEnableDebugLoggingWhenDebugIsTrue() {
        when(loggingAdminService.getLoggerConfiguration("com.example")).thenReturn(null);

        controller.sendMessages("test message", 1, 1, 0, new PerfController.ExportOptions(),
                new PerfController.RunOptions(null, true, null));

        Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .pollInterval(Duration.ofMillis(100))
                .untilAsserted(() -> verify(loggingAdminService).setLogLevel("com.example", LogLevel.DEBUG));
    }

    @Test
    void exportOptionsSettersCoverSpringMvcBinding() {
        var opts = new PerfController.ExportOptions();
        opts.setExportGrafana(true);
        opts.setExportPrometheus(true);
        opts.setExportKubernetes(true);
        opts.setExportLogs(true);
        assertTrue(opts.exportGrafana());
        assertTrue(opts.exportPrometheus());
        assertTrue(opts.exportKubernetes());
        assertTrue(opts.exportLogs());
    }

    @Test
    void runOptionsSettersCoverSpringMvcBinding() {
        var opts = new PerfController.RunOptions();
        opts.setTestId("tid");
        opts.setDebug(true);
        opts.setScenarioId(5L);
        assertEquals("tid", opts.testId());
        assertTrue(opts.debug());
        assertEquals(5L, opts.scenarioId());
    }

    @Test
    void streamProgressShouldReturnSseEmitter() {
        var emitter = controller.streamProgress("some-test-run-id");

        assertNotNull(emitter);
    }

    @Test
    void sendMessagesShouldUseScenarioPoolWhenScenarioIdProvided() {
        var scenarioMsg = new TestScenarioService.ScenarioMessage("scenario-payload", Map.of());
        when(testScenarioService.getScenarioCount(1L)).thenReturn(3);
        when(testScenarioService.buildMessagePool(1L)).thenReturn(
                List.of(scenarioMsg, scenarioMsg, scenarioMsg));
        when(testScenarioService.getById(1L)).thenReturn(
                new TestScenarioService.TestScenarioDetail(1L, "test", 3, List.of(),
                        false, null, 0, null, null, List.of(), "now", "now"));

        controller.sendMessages(null, 1000, 1, 0,
                new PerfController.ExportOptions(), new PerfController.RunOptions(null, false, 1L));

        Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .pollInterval(Duration.ofMillis(100))
                .untilAsserted(() -> verify(messageSender, times(3)).sendMessage(anyString()));
    }

    @Test
    void sendMessagesShouldExportPrometheusWhenPrometheusExportEnabled() throws IOException {
        var promFile = tempDir.resolve("prometheus.json");
        Files.writeString(promFile, "{}");
        var prometheusExport = new PrometheusExportResult(
                promFile.toString(), "http://prometheus/query", null);
        when(prometheusExportService.exportMetrics(anyLong(), anyLong(), any())).thenReturn(prometheusExport);

        Path tempZip = tempDir.resolve("test.zip");
        Files.write(tempZip, "content".getBytes());
        when(testResultPackager.packageResults(any(), anyList(), anyString(), anyList(), any(), anyLong(), anyLong()))
                .thenReturn(new PackageResult("test.zip", tempZip.toString()));

        controller.sendMessages("test message", 1, 1, 0, new PerfController.ExportOptions(false, true, false, false),
                new PerfController.RunOptions("test-id", false, null));

        Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .pollInterval(Duration.ofMillis(100))
                .untilAsserted(() -> verify(prometheusExportService).exportMetrics(anyLong(), anyLong(), any()));
    }

    @Test
    void sendMessagesShouldExecuteWarmupPhaseWhenWarmupCountIsPositive() throws Exception {
        var detail = new TestScenarioService.TestScenarioDetail(
                1L, "test", 3, List.of(), false, null, 5, null, null, List.of(), "now", "now");
        when(testScenarioService.getScenarioCount(1L)).thenReturn(3);
        when(testScenarioService.getById(1L)).thenReturn(detail);
        when(testScenarioService.buildMessagePool(1L)).thenReturn(List.of());
        doReturn(true).when(performanceTracker).awaitWarmupCompletion(anyLong(), any(TimeUnit.class));

        controller.sendMessages(null, 3, 60, 0,
                new PerfController.ExportOptions(), new PerfController.RunOptions(null, false, 1L));

        Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .pollInterval(Duration.ofMillis(100))
                .untilAsserted(() -> verify(performanceTracker).startWarmupPhase(5));
    }

    @Test
    void sendMessagesShouldEvaluateThresholdsForScenario() {
        var thresholds = List.of(new ThresholdDef("TPS", "GTE", 5.0));
        var thresholdResults = List.of(new ThresholdResult("TPS", "GTE", 5.0, 10.0, true));
        var detail = new TestScenarioService.TestScenarioDetail(
                1L, "test", 3, List.of(), false, null, 0, null, null, List.of(), "now", "now");
        when(testScenarioService.getScenarioCount(1L)).thenReturn(3);
        when(testScenarioService.getById(1L)).thenReturn(detail);
        when(testScenarioService.buildMessagePool(1L)).thenReturn(List.of());
        when(testScenarioService.getScenarioThresholds(1L)).thenReturn(thresholds);
        when(thresholdEvaluator.evaluate(thresholds, performanceTracker.getResult()))
                .thenReturn(thresholdResults);

        controller.sendMessages(null, 3, 60, 0,
                new PerfController.ExportOptions(), new PerfController.RunOptions(null, false, 1L));

        Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .pollInterval(Duration.ofMillis(100))
                .untilAsserted(() -> verify(testRunService).updateThresholdResult(anyLong(), any(), any()));
    }

    @Test
    void sendMessagesShouldExportLogsWhenLogsExportEnabled() throws IOException {
        var logEntries = List.of(new LogEntry("2024-01-01T00:00:00Z", "INFO", "test log message"));
        when(lokiService.queryLogs(any(), any())).thenReturn(logEntries);

        Path tempZip = tempDir.resolve("test.zip");
        Files.write(tempZip, "content".getBytes());
        when(testResultPackager.packageResults(any(), anyList(), any(), anyList(), any(), anyLong(), anyLong()))
                .thenReturn(new PackageResult("test.zip", tempZip.toString()));

        controller.sendMessages("test message", 1, 1, 0, new PerfController.ExportOptions(false, false, false, true),
                new PerfController.RunOptions("test-id", false, null));

        Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .pollInterval(Duration.ofMillis(100))
                .untilAsserted(() -> verify(lokiService).queryLogs(any(), any()));
    }
}
