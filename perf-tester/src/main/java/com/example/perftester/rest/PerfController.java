package com.example.perftester.rest;

import com.example.perftester.export.TestResultPackager;
import com.example.perftester.export.TestResultPackager.PackageResult;
import com.example.perftester.grafana.GrafanaExportService;
import com.example.perftester.kubernetes.KubernetesService;
import com.example.perftester.messaging.MessageSender;
import com.example.perftester.perf.PerfTestResult;
import com.example.perftester.perf.PerformanceTracker;
import com.example.perftester.prometheus.PrometheusExportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
@RestController
@RequestMapping("/api/perf")
@RequiredArgsConstructor
public class PerfController {

    private static final int METRICS_PROPAGATION_DELAY_MS = 16000;

    private final MessageSender messageSender;
    private final PerformanceTracker performanceTracker;
    private final GrafanaExportService grafanaExportService;
    private final PrometheusExportService prometheusExportService;
    private final TestResultPackager testResultPackager;
    private final KubernetesService kubernetesService;

    @PostMapping("/send")
    public ResponseEntity<Resource> sendMessages(
            @RequestBody String message,
            @RequestParam(defaultValue = "1000") int count,
            @RequestParam(defaultValue = "60") int timeoutSeconds,
            @RequestParam(defaultValue = "0") int delayMs,
            @RequestParam(required = false) String testId,
            @RequestParam(defaultValue = "false") boolean exportStatistics) throws InterruptedException {

        log.info("Starting performance test: {} messages, timeout {}s, delay {}ms, testId={}",
                count, timeoutSeconds, delayMs, testId);

        long testStartTimeMs = System.currentTimeMillis();
        var result = runPerformanceTest(message, count, timeoutSeconds, delayMs);
        long testEndTimeMs = System.currentTimeMillis();

        if (exportStatistics) {
            // Export Kubernetes cluster info and wait for metrics propagation
            result = result.withKubernetesExport(kubernetesService.exportClusterInfo());
            Thread.sleep(METRICS_PROPAGATION_DELAY_MS);

            // Export dashboards and metrics
            var exports = exportTestArtifacts(result, testStartTimeMs, testEndTimeMs, testId);

            // Package and return response
            var packageResult = testResultPackager.packageResults(
                    exports.result(), exports.dashboardFiles(), exports.prometheusFile(),
                    testId, testStartTimeMs, testEndTimeMs);

            log.info("Test results packaged: {} ({})", packageResult.filename(), packageResult.savedPath());
            cleanupExportedFiles(exports.dashboardFiles(), exports.prometheusFile(),
                    exports.result().kubernetesExportFile());

            return buildZipResponse(packageResult);
        }
        return ResponseEntity.ok().build();
    }

    private PerfTestResult runPerformanceTest(String message, int count, int timeoutSeconds, int delayMs)
            throws InterruptedException {
        performanceTracker.startTest(count);

        var futures = new CompletableFuture<?>[count];
        for (int i = 0; i < count; i++) {
            var payload = message + "-" + i;
            futures[i] = messageSender.sendMessage(payload);
            if (delayMs > 0) {
                Thread.sleep(delayMs);
            }
        }
        // this is here to wait untill all messages are send
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
        return result;
    }

    private ExportContext exportTestArtifacts(PerfTestResult testResult, long startTime, long endTime, String testId) {
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

    private ResponseEntity<Resource> buildZipResponse(PackageResult packageResult) {
        var zipPath = Path.of(packageResult.savedPath());
        var resource = new FileSystemResource(zipPath);

        try {
            long fileSize = Files.size(zipPath);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + packageResult.filename() + "\"")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .contentLength(fileSize)
                    .body(resource);
        } catch (IOException e) {
            log.error("Failed to get file size: {}", e.getMessage());
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + packageResult.filename() + "\"")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(resource);
        }
    }

    private record ExportContext(PerfTestResult result, List<String> dashboardFiles, String prometheusFile) {
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
