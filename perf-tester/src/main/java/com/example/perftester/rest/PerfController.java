package com.example.perftester.rest;

import com.example.perftester.admin.LoggingAdminService;
import com.example.perftester.config.PerfProperties;
import com.example.perftester.export.TestResultPackager;
import com.example.perftester.grafana.GrafanaExportService;
import com.example.perftester.kubernetes.KubernetesService;
import com.example.perftester.messaging.MessageSender;
import com.example.perftester.perf.PerfTestResult;
import com.example.perftester.perf.PerformanceTracker;
import com.example.perftester.perf.TestStartResponse;
import com.example.perftester.persistence.TestRunService;
import com.example.perftester.prometheus.PrometheusExportService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.logging.LogLevel;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
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
    private final LoggingAdminService loggingAdminService;
    private final PerfProperties perfProperties;
    private final TestRunService testRunService;

    /**
     * Starts a performance test asynchronously and returns a testRunId immediately.
     * Progress can be streamed via GET /api/perf/progress/{testRunId}.
     */
    @PostMapping("/send")
    public ResponseEntity<TestStartResponse> sendMessages(
            @RequestBody @NotBlank String message,
            @RequestParam(defaultValue = "1000") @Min(1) @Max(100000) int count,
            @RequestParam(defaultValue = "60") @Min(1) @Max(3600) int timeoutSeconds,
            @RequestParam(defaultValue = "0") @Min(0) @Max(60000) int delayMs,
            @RequestParam(required = false) String testId,
            @RequestParam(defaultValue = "false") boolean exportStatistics,
            @RequestParam(defaultValue = "false") boolean debug) {

        var testRunId = UUID.randomUUID().toString();
        var request = new TestRunRequest(testRunId, message, count, timeoutSeconds, delayMs, testId, exportStatistics, debug);

        Thread.ofVirtual().name("perf-test-" + testRunId).start(() -> runTestInBackground(request));

        return ResponseEntity.accepted().body(new TestStartResponse(testRunId));
    }

    /**
     * Streams real-time test progress as Server-Sent Events.
     * Sends a progress update every 500ms until the test completes, times out, or fails.
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
        var testRunEntity = testRunService.createRun(req.testRunId(), req.testId(), req.count());
        try {
            log.info("Starting async performance test: testRunId={}, count={}, timeout={}s, delay={}ms, testId={}",
                    req.testRunId(), req.count(), req.timeoutSeconds(), req.delayMs(), req.testId());

            performanceTracker.startTest(req.count(), req.testRunId());
            long testStartTimeMs = System.currentTimeMillis();

            var completed = runPerformanceTest(req.message(), req.count(), req.timeoutSeconds(), req.delayMs());
            long testEndTimeMs = System.currentTimeMillis();

            var finalStatus = completed ? "COMPLETED" : "TIMEOUT";
            performanceTracker.setStatus(finalStatus);
            var result = performanceTracker.getResult();

            log.info("Test {}: testRunId={}, {}/{} messages, TPS={}, avgLatency={}ms",
                    completed ? "completed" : "timed out",
                    req.testRunId(), result.completedMessages(), req.count(),
                    String.format("%.2f", result.tps()),
                    String.format("%.2f", result.avgLatencyMs()));

            String zipPath = null;
            if (req.exportStatistics()) {
                result = result.withKubernetesExport(kubernetesService.exportClusterInfo());
                Thread.sleep(perfProperties.metricsPropagationDelayMs());
                var exports = exportTestArtifacts(result, testStartTimeMs, testEndTimeMs, req.testId());
                var packageResult = testResultPackager.packageResults(
                        exports.result(), exports.dashboardFiles(), exports.prometheusFile(),
                        req.testId(), testStartTimeMs, testEndTimeMs);
                zipPath = packageResult.savedPath();
                log.info("Test results packaged: {} ({})", packageResult.filename(), zipPath);
                cleanupExportedFiles(exports.dashboardFiles(), exports.prometheusFile(),
                        exports.result().kubernetesExportFile());
            }
            testRunService.completeRun(testRunEntity.getId(), finalStatus, result, zipPath);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            performanceTracker.setStatus("FAILED");
            testRunService.completeRun(testRunEntity.getId(), "FAILED", performanceTracker.getResult(), null);
            log.error("Performance test interrupted: testRunId={}", req.testRunId());
        } catch (Exception e) {
            performanceTracker.setStatus("FAILED");
            testRunService.completeRun(testRunEntity.getId(), "FAILED", performanceTracker.getResult(), null);
            log.error("Performance test failed: testRunId={}", req.testRunId(), e);
        } finally {
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

    private boolean runPerformanceTest(String message, int count, int timeoutSeconds, int delayMs)
            throws InterruptedException {
        var futures = new CompletableFuture<?>[count];
        for (int i = 0; i < count; i++) {
            var payload = message + "-" + i;
            futures[i] = messageSender.sendMessage(payload);
            if (delayMs > 0) {
                Thread.sleep(delayMs);
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
                                              long startTime, long endTime, String testId) {
        log.info("Exporting Grafana dashboards...");
        var dashboardExport = grafanaExportService.exportDashboards(startTime, endTime);
        dashboardExport.dashboardUrls().forEach(url -> log.info("  Dashboard URL: {}", url));
        var dashboardFiles = new ArrayList<>(dashboardExport.exportedFiles());
        var enrichedResult = testResult.withDashboardExports(
                dashboardExport.dashboardUrls(), dashboardExport.exportedFiles());

        log.info("Exporting Prometheus metrics...");
        var prometheusExport = prometheusExportService.exportMetrics(startTime, endTime, testId);
        String prometheusFile = null;
        if (prometheusExport.isSuccess()) {
            log.info("Prometheus metrics exported to: {}", prometheusExport.filePath());
            prometheusFile = prometheusExport.filePath();
            enrichedResult = enrichedResult.withPrometheusExport(prometheusFile);
        } else {
            log.warn("Failed to export Prometheus metrics: {}", prometheusExport.error());
        }

        return new ExportContext(enrichedResult, dashboardFiles, prometheusFile);
    }

    private record ExportContext(PerfTestResult result, List<String> dashboardFiles, String prometheusFile) {
    }

    private record TestRunRequest(String testRunId, String message, int count, int timeoutSeconds,
                                  int delayMs, String testId, boolean exportStatistics, boolean debug) {
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

        int fileCount = dashboardFiles.size() + (prometheusFile != null ? 1 : 0) + (kubernetesFile != null ? 1 : 0);
        log.info("Cleaned up {} exported files", fileCount);
    }

}
