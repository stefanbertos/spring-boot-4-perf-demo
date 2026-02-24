package com.example.perftester.rest;

import com.example.perftester.admin.LoggingAdminService;
import com.example.perftester.config.PerfProperties;
import com.example.perftester.export.TestResultPackager;
import com.example.perftester.grafana.GrafanaExportService;
import com.example.perftester.kubernetes.KubernetesService;
import com.example.perftester.loki.LogEntry;
import com.example.perftester.loki.LokiService;
import com.example.perftester.messaging.MessageSender;
import com.example.perftester.monitoring.InfraSnapshotService;
import com.example.perftester.perf.PerfTestResult;
import com.example.perftester.perf.PerformanceTracker;
import com.example.perftester.perf.ThinkTimeCalculator;
import com.example.perftester.perf.ThinkTimeConfig;
import com.example.perftester.perf.ThresholdEvaluator;
import com.example.perftester.perf.ThresholdResult;
import com.example.perftester.perf.TestStartResponse;
import com.example.perftester.persistence.TestRunService;
import com.example.perftester.persistence.TestScenarioService;
import com.example.perftester.prometheus.PrometheusExportService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.logging.LogLevel;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;


@Slf4j
@Validated
@RestController
@RequestMapping("/api/perf")
@RequiredArgsConstructor
@EnableConfigurationProperties(PerfProperties.class)
public class PerfController {

    private static final String DEBUG_LOGGER = "com.example";
    private static final long SSE_TIMEOUT_MS = 600_000L;
    private static final long SSE_POLL_INTERVAL_MS = 500L;

    private final MessageSender messageSender;
    private final PerformanceTracker performanceTracker;
    private final GrafanaExportService grafanaExportService;
    private final PrometheusExportService prometheusExportService;
    private final TestResultPackager testResultPackager;
    private final KubernetesService kubernetesService;
    private final LokiService lokiService;
    private final LoggingAdminService loggingAdminService;
    private final PerfProperties perfProperties;
    private final TestRunService testRunService;
    private final TestScenarioService testScenarioService;
    private final ThinkTimeCalculator thinkTimeCalculator;
    private final ThresholdEvaluator thresholdEvaluator;
    private final InfraSnapshotService infraSnapshotService;

    /**
     * Starts a performance test asynchronously and returns a testRunId immediately.
     * Progress can be streamed via GET /api/perf/progress/{testRunId}.
     */
    @PostMapping("/send")
    public ResponseEntity<TestStartResponse> sendMessages(
            @RequestBody(required = false) String message,
            @RequestParam(defaultValue = "1000") @Min(1) @Max(100000) int count,
            @RequestParam(defaultValue = "60") @Min(1) @Max(3600) int timeoutSeconds,
            @RequestParam(defaultValue = "0") @Min(0) @Max(60000) int delayMs,
            @ModelAttribute ExportOptions exportOptions,
            @ModelAttribute RunOptions runOptions) {

        var testRunId = UUID.randomUUID().toString();
        int effectiveCount = runOptions.scenarioId() != null
                ? testScenarioService.getScenarioCount(runOptions.scenarioId()) : count;

        String testType = null;
        int warmupCount = 0;
        ThinkTimeConfig thinkTimeConfig = null;
        if (runOptions.scenarioId() != null) {
            var scenario = testScenarioService.getById(runOptions.scenarioId());
            testType = scenario.testType();
            warmupCount = scenario.warmupCount();
            thinkTimeConfig = scenario.thinkTime();
        }

        var testRunEntity = testRunService.createRun(testRunId, runOptions.testId(), effectiveCount, testType);
        var effectiveMessage = message != null ? message : "";
        var request = new TestRunRequest(
                testRunEntity.getId(), testRunId, effectiveMessage, effectiveCount, timeoutSeconds, delayMs,
                runOptions.testId(), exportOptions.exportGrafana(), exportOptions.exportPrometheus(),
                exportOptions.exportKubernetes(), exportOptions.exportLogs(),
                runOptions.debug(), runOptions.scenarioId(), warmupCount, thinkTimeConfig);

        Thread.ofVirtual().name("perf-test-" + testRunId).start(() -> runTestInBackground(request));

        return ResponseEntity.accepted().body(new TestStartResponse(testRunEntity.getId(), testRunId));
    }

    /**
     * Streams real-time test progress as Server-Sent Events.
     * Sends a progress update every 500ms until the test completes, times out, or fails.
     * The stream remains open during the EXPORTING phase (when export flags are enabled).
     */
    @GetMapping(value = "/progress/{testRunId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamProgress(@PathVariable String testRunId) {
        var emitter = new SseEmitter(SSE_TIMEOUT_MS);
        var scheduler = Executors.newSingleThreadScheduledExecutor(
                Thread.ofVirtual().factory());

        var future = scheduler.scheduleAtFixedRate(() -> {
            try {
                var snapshot = performanceTracker.getProgressSnapshot();
                emitter.send(SseEmitter.event().data(snapshot, MediaType.APPLICATION_JSON));
                var status = snapshot.status();
                if ("COMPLETED".equals(status) || "TIMEOUT".equals(status) || "FAILED".equals(status)) {
                    emitter.complete();
                }
            } catch (IOException e) {
                emitter.completeWithError(e);
            }
        }, 0, SSE_POLL_INTERVAL_MS, TimeUnit.MILLISECONDS);

        Runnable cleanup = () -> {
            future.cancel(true);
            scheduler.shutdown();
        };
        emitter.onCompletion(cleanup);
        emitter.onError(e -> cleanup.run());
        emitter.onTimeout(cleanup);

        return emitter;
    }

    private void runTestInBackground(TestRunRequest req) {
        LogLevel previousLevel = null;
        if (req.debug()) {
            previousLevel = enableDebugLogging();
        }
        try {
            log.info("Starting async performance test: testRunId={}, count={}, timeout={}s, delay={}ms, testId={}",
                    req.testRunId(), req.count(), req.timeoutSeconds(), req.delayMs(), req.testId());

            if (req.warmupCount() > 0) {
                performanceTracker.startWarmupPhase(req.warmupCount());
                for (int i = 0; i < req.warmupCount(); i++) {
                    messageSender.sendMessage("warmup-" + i);
                }
                performanceTracker.awaitWarmupCompletion(60, TimeUnit.SECONDS);
                log.info("Warmup phase complete: {} messages", req.warmupCount());
            }

            performanceTracker.startTest(req.count(), req.testRunId());
            infraSnapshotService.startMonitoring(req.entityId());
            long testStartTimeMs = System.currentTimeMillis();

            var completed = runPerformanceTest(req.message(), req.count(),
                    req.timeoutSeconds(), req.delayMs(), req.scenarioId(), req.thinkTimeConfig());
            long testEndTimeMs = System.currentTimeMillis();

            var finalStatus = completed ? "COMPLETED" : "TIMEOUT";
            var result = performanceTracker.getResult();

            log.info("Test {}: testRunId={}, {}/{} messages, TPS={}, avgLatency={}ms",
                    completed ? "completed" : "timed out",
                    req.testRunId(), result.completedMessages(), req.count(),
                    String.format("%.2f", result.tps()),
                    String.format("%.2f", result.avgLatencyMs()));

            String zipPath = null;
            if (req.anyExport()) {
                performanceTracker.setStatus("EXPORTING");
                if (req.exportKubernetes()) {
                    result = result.withKubernetesExport(kubernetesService.exportClusterInfo());
                }
                Thread.sleep(perfProperties.metricsPropagationDelayMs());
                var exports = exportTestArtifacts(result, testStartTimeMs, testEndTimeMs, req.testId(),
                        req.exportGrafana(), req.exportPrometheus(), req.exportLogs());
                var packageResult = testResultPackager.packageResults(
                        exports.result(), exports.dashboardFiles(), exports.prometheusFile(),
                        exports.logEntries(), req.testId(), testStartTimeMs, testEndTimeMs);
                zipPath = packageResult.savedPath();
                log.info("Test results packaged: {} ({})", packageResult.filename(), zipPath);
                cleanupExportedFiles(exports.dashboardFiles(), exports.prometheusFile(),
                        exports.result().kubernetesExportFile());
            }
            performanceTracker.setStatus(finalStatus);
            testRunService.completeRun(req.entityId(), finalStatus, result, zipPath);

            if (req.scenarioId() != null) {
                var thresholds = testScenarioService.getScenarioThresholds(req.scenarioId());
                if (!thresholds.isEmpty()) {
                    var thresholdResults = thresholdEvaluator.evaluate(thresholds, result);
                    var passed = thresholdResults.stream().allMatch(ThresholdResult::passed);
                    testRunService.updateThresholdResult(req.entityId(),
                            passed ? "PASSED" : "FAILED", thresholdResults);
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            performanceTracker.setStatus("FAILED");
            testRunService.completeRun(req.entityId(), "FAILED", performanceTracker.getResult(), null);
            log.error("Performance test interrupted: testRunId={}", req.testRunId());
        } catch (Exception e) {
            performanceTracker.setStatus("FAILED");
            testRunService.completeRun(req.entityId(), "FAILED", performanceTracker.getResult(), null);
            log.error("Performance test failed: testRunId={}", req.testRunId(), e);
        } finally {
            infraSnapshotService.stopMonitoring();
            if (req.debug() && previousLevel != null) {
                loggingAdminService.setLogLevel(DEBUG_LOGGER, previousLevel);
            }
        }
    }

    private LogLevel enableDebugLogging() {
        var config = loggingAdminService.getLoggerConfiguration(DEBUG_LOGGER);
        var previousLevel = config != null ? config.getEffectiveLevel() : LogLevel.INFO;
        loggingAdminService.setLogLevel(DEBUG_LOGGER, LogLevel.DEBUG);
        log.info("Debug mode enabled for '{}' (previous level: {})", DEBUG_LOGGER, previousLevel);
        return previousLevel;
    }

    private boolean runPerformanceTest(String message, int count, int timeoutSeconds, int delayMs,
                                       Long scenarioId, ThinkTimeConfig thinkTimeConfig)
            throws InterruptedException {
        List<TestScenarioService.ScenarioMessage> pool = scenarioId != null
                ? testScenarioService.buildMessagePool(scenarioId) : List.of();
        var futures = new CompletableFuture<?>[count];
        for (int i = 0; i < count; i++) {
            if (!pool.isEmpty()) {
                var msg = pool.get(i);
                futures[i] = msg.jmsProperties().isEmpty()
                        ? messageSender.sendMessage(msg.content() + "-" + i)
                        : messageSender.sendMessage(msg.content() + "-" + i, msg.jmsProperties());
            } else {
                futures[i] = messageSender.sendMessage(message + "-" + i);
            }
            long sleepMs = thinkTimeConfig != null
                    ? thinkTimeCalculator.nextSleepMs(thinkTimeConfig) : (long) delayMs;
            if (sleepMs > 0) {
                Thread.sleep(sleepMs);
            }
        }
        CompletableFuture.allOf(futures).join();

        log.info("All {} messages sent, waiting for responses...", count);
        var completed = performanceTracker.awaitCompletion(timeoutSeconds, TimeUnit.SECONDS);
        var result = performanceTracker.getResult();

        if (completed) {
            log.info("Test completed: {} messages, TPS={}, avgLatency={}ms",
                    result.completedMessages(), String.format("%.2f", result.tps()),
                    String.format("%.2f", result.avgLatencyMs()));
        } else {
            log.warn("Test timed out: {}/{} messages completed", result.completedMessages(), count);
        }
        return completed;
    }

    private ExportContext exportTestArtifacts(PerfTestResult testResult,
                                              long startTime, long endTime, String testId,
                                              boolean exportGrafana, boolean exportPrometheus,
                                              boolean exportLogs) {
        var enrichedResult = testResult;
        var dashboardFiles = new ArrayList<String>();
        String prometheusFile = null;
        List<LogEntry> logEntries = List.of();

        if (exportGrafana) {
            log.info("Exporting Grafana dashboards...");
            var dashboardExport = grafanaExportService.exportDashboards(startTime, endTime);
            dashboardExport.dashboardUrls().forEach(url -> log.info("  Dashboard URL: {}", url));
            dashboardFiles.addAll(dashboardExport.exportedFiles());
            enrichedResult = enrichedResult.withDashboardExports(
                    dashboardExport.dashboardUrls(), dashboardExport.exportedFiles());
        }

        if (exportPrometheus) {
            log.info("Exporting Prometheus metrics...");
            var prometheusExport = prometheusExportService.exportMetrics(startTime, endTime, testId);
            if (prometheusExport.isSuccess()) {
                log.info("Prometheus metrics exported to: {}", prometheusExport.filePath());
                prometheusFile = prometheusExport.filePath();
                enrichedResult = enrichedResult.withPrometheusExport(prometheusFile);
            } else {
                log.warn("Failed to export Prometheus metrics: {}", prometheusExport.error());
            }
        }

        if (exportLogs) {
            log.info("Exporting application logs from Loki...");
            logEntries = lokiService.queryLogs(Instant.ofEpochMilli(startTime), Instant.ofEpochMilli(endTime));
            log.info("Exported {} log entries from Loki", logEntries.size());
        }

        return new ExportContext(enrichedResult, dashboardFiles, prometheusFile, logEntries);
    }

    /**
     * Holds the individual export flags submitted with a test run request.
     * Spring MVC binds these from query parameters via @ModelAttribute.
     * Using a mutable class (not a record) so Spring can create via no-arg constructor
     * and only set fields present in the request, leaving absent params as false.
     */
    public static class ExportOptions {
        private boolean exportGrafana;
        private boolean exportPrometheus;
        private boolean exportKubernetes;
        private boolean exportLogs;

        public ExportOptions() {
            // Used by Spring MVC @ModelAttribute binding.
        }

        public ExportOptions(boolean exportGrafana, boolean exportPrometheus,
                             boolean exportKubernetes, boolean exportLogs) {
            this.exportGrafana = exportGrafana;
            this.exportPrometheus = exportPrometheus;
            this.exportKubernetes = exportKubernetes;
            this.exportLogs = exportLogs;
        }

        public boolean exportGrafana() {
            return exportGrafana;
        }

        public void setExportGrafana(boolean exportGrafana) {
            this.exportGrafana = exportGrafana;
        }

        public boolean exportPrometheus() {
            return exportPrometheus;
        }

        public void setExportPrometheus(boolean exportPrometheus) {
            this.exportPrometheus = exportPrometheus;
        }

        public boolean exportKubernetes() {
            return exportKubernetes;
        }

        public void setExportKubernetes(boolean exportKubernetes) {
            this.exportKubernetes = exportKubernetes;
        }

        public boolean exportLogs() {
            return exportLogs;
        }

        public void setExportLogs(boolean exportLogs) {
            this.exportLogs = exportLogs;
        }
    }

    /**
     * Holds run-specific options (testId, debug flag, scenarioId).
     * Spring MVC binds these from query parameters via @ModelAttribute.
     * Using a mutable class (not a record) so Spring can create via no-arg constructor
     * and only set fields present in the request, leaving absent params as defaults.
     */
    public static class RunOptions {
        private String testId;
        private boolean debug;
        private Long scenarioId;

        public RunOptions() {
            // Used by Spring MVC @ModelAttribute binding.
        }

        public RunOptions(String testId, boolean debug, Long scenarioId) {
            this.testId = testId;
            this.debug = debug;
            this.scenarioId = scenarioId;
        }

        public String testId() {
            return testId;
        }

        public void setTestId(String testId) {
            this.testId = testId;
        }

        public boolean debug() {
            return debug;
        }

        public void setDebug(boolean debug) {
            this.debug = debug;
        }

        public Long scenarioId() {
            return scenarioId;
        }

        public void setScenarioId(Long scenarioId) {
            this.scenarioId = scenarioId;
        }
    }

    private record ExportContext(PerfTestResult result, List<String> dashboardFiles,
                                 String prometheusFile, List<LogEntry> logEntries) {
    }

    private record TestRunRequest(Long entityId, String testRunId, String message, int count,
                                  int timeoutSeconds, int delayMs, String testId,
                                  boolean exportGrafana, boolean exportPrometheus,
                                  boolean exportKubernetes, boolean exportLogs,
                                  boolean debug, Long scenarioId,
                                  int warmupCount, ThinkTimeConfig thinkTimeConfig) {
        boolean anyExport() {
            return exportGrafana || exportPrometheus || exportKubernetes || exportLogs;
        }
    }

    private void cleanupExportedFiles(List<String> dashboardFiles, String prometheusFile,
                                      String kubernetesFile) {
        for (var file : dashboardFiles) {
            try {
                Files.deleteIfExists(Path.of(file));
                log.debug("Deleted dashboard export: {}", file);
            } catch (IOException e) {
                log.warn("Failed to delete dashboard export {}: {}", file, e.getMessage());
            }
        }

        if (prometheusFile != null) {
            try {
                Files.deleteIfExists(Path.of(prometheusFile));
                log.debug("Deleted prometheus export: {}", prometheusFile);
            } catch (IOException e) {
                log.warn("Failed to delete prometheus export {}: {}", prometheusFile, e.getMessage());
            }
        }

        if (kubernetesFile != null) {
            try {
                var kubernetesPath = Path.of(kubernetesFile);
                if (Files.isDirectory(kubernetesPath)) {
                    try (var dirFiles = Files.list(kubernetesPath)) {
                        dirFiles.forEach(file -> {
                            try {
                                Files.deleteIfExists(file);
                            } catch (IOException ex) {
                                log.warn("Failed to delete kubernetes file {}: {}", file, ex.getMessage());
                            }
                        });
                    }
                    Files.deleteIfExists(kubernetesPath);
                } else {
                    Files.deleteIfExists(kubernetesPath);
                }
                log.debug("Deleted kubernetes export: {}", kubernetesFile);
            } catch (IOException e) {
                log.warn("Failed to delete kubernetes export {}: {}", kubernetesFile, e.getMessage());
            }
        }

        int fileCount = dashboardFiles.size() + (prometheusFile != null ? 1 : 0)
                + (kubernetesFile != null ? 1 : 0);
        log.info("Cleaned up {} exported files", fileCount);
    }

}
