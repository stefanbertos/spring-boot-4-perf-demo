package com.example.perftester.rest;

import com.example.perftester.export.TestResultPackager;
import com.example.perftester.export.TestResultPackager.PackageResult;
import com.example.perftester.grafana.GrafanaExportService;
import com.example.perftester.grafana.GrafanaExportService.DashboardExportResult;
import com.example.perftester.messaging.MessageSender;
import com.example.perftester.perf.PerfTestResult;
import com.example.perftester.perf.PerformanceTracker;
import com.example.perftester.prometheus.PrometheusExportService;
import com.example.perftester.prometheus.PrometheusExportService.PrometheusExportResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
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
import java.util.concurrent.TimeUnit;

@Slf4j
@RestController
@RequestMapping("/api/perf")
@RequiredArgsConstructor
public class PerfController {

    private final MessageSender messageSender;
    private final PerformanceTracker performanceTracker;
    private final GrafanaExportService grafanaExportService;
    private final PrometheusExportService prometheusExportService;
    private final TestResultPackager testResultPackager;

    @PostMapping("/send")
    public ResponseEntity<Resource> sendMessages(
            @RequestBody String message,
            @RequestParam(defaultValue = "1000") int count,
            @RequestParam(defaultValue = "60") int timeoutSeconds,
            @RequestParam(defaultValue = "0") int delayMs,
            @RequestParam(required = false) String testId) throws InterruptedException {

        log.info("Starting performance test: {} messages, timeout {}s, delay {}ms, testId={}",
                count, timeoutSeconds, delayMs, testId);

        long testStartTimeMs = System.currentTimeMillis();
        performanceTracker.startTest(count);

        for (int i = 0; i < count; i++) {
            messageSender.sendMessage(message + "-" + i);
            if (delayMs > 0) {
                Thread.sleep(delayMs);
            }
        }

        log.info("All {} messages sent, waiting for responses...", count);

        boolean completed = performanceTracker.awaitCompletion(timeoutSeconds, TimeUnit.SECONDS);
        long testEndTimeMs = System.currentTimeMillis();

        PerfTestResult result = performanceTracker.getResult();

        if (completed) {
            log.info("Test completed: {} messages, TPS={}, avgLatency={}ms",
                    result.completedMessages(), String.format("%.2f", result.tps()),
                    String.format("%.2f", result.avgLatencyMs()));
        } else {
            log.warn("Test timed out: {}/{} messages completed", result.completedMessages(), count);
        }

        Thread.sleep(16000); // sleep 2 minutes to propagate metrics

        // Export Grafana dashboards
        List<String> dashboardFiles = new ArrayList<>();
        log.info("Exporting Grafana dashboards...");
        DashboardExportResult dashboardExport = grafanaExportService.exportDashboards(testStartTimeMs, testEndTimeMs);

        log.info("Dashboard URLs:");
        dashboardExport.dashboardUrls().forEach(url -> log.info("  {}", url));

        if (!dashboardExport.exportedFiles().isEmpty()) {
            log.info("Dashboard files:");
            dashboardExport.exportedFiles().forEach(file -> log.info("  {}", file));
            dashboardFiles.addAll(dashboardExport.exportedFiles());
        }

        result = result.withDashboardExports(dashboardExport.dashboardUrls(), dashboardExport.exportedFiles());

        // Export Prometheus metrics
        String prometheusFile = null;
        log.info("Exporting Prometheus metrics...");
        PrometheusExportResult prometheusExport = prometheusExportService.exportMetrics(
                testStartTimeMs, testEndTimeMs, testId);

        if (prometheusExport.isSuccess()) {
            log.info("Prometheus metrics exported to: {}", prometheusExport.filePath());
            log.info("Query URL pattern: {}", prometheusExport.queryUrl());
            prometheusFile = prometheusExport.filePath();
            result = result.withPrometheusExport(prometheusFile);
        } else {
            log.warn("Failed to export Prometheus metrics: {}", prometheusExport.error());
        }

        // Package everything into a ZIP file
        log.info("Packaging test results...");
        PackageResult packageResult = testResultPackager.packageResults(
                result,
                dashboardFiles,
                prometheusFile,
                testId,
                testStartTimeMs,
                testEndTimeMs
        );

        log.info("Test results packaged: {} ({})", packageResult.filename(), packageResult.savedPath());

        // Clean up exported files after packaging
        cleanupExportedFiles(dashboardFiles, prometheusFile);

        var resource = new ByteArrayResource(packageResult.zipBytes());

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + packageResult.filename() + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .contentLength(packageResult.zipBytes().length)
                .body(resource);
    }

    private void cleanupExportedFiles(List<String> dashboardFiles, String prometheusFile) {
        for (String file : dashboardFiles) {
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

        log.info("Cleaned up {} exported files", dashboardFiles.size() + (prometheusFile != null ? 1 : 0));
    }
}
