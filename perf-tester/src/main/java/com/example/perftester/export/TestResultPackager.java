package com.example.perftester.export;

import com.example.perftester.loki.LogEntry;
import com.example.perftester.perf.PerfTestResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Slf4j
@Service
public class TestResultPackager {

    private final String exportPath;

    public TestResultPackager(@Value("${app.export.path:./test-exports}") String exportPath) {
        this.exportPath = exportPath;
    }

    /**
     * Packages test results, dashboard images, Prometheus metrics, and Kubernetes info
     * into a single ZIP file saved to the configured export path.
     *
     * @param result               performance test metrics (TPS, latency, completion counts)
     * @param dashboardImageFiles  paths to exported Grafana dashboard PNG files
     * @param prometheusExportFile path to exported Prometheus metrics JSON (nullable)
     * @param testId               optional test identifier for the filename
     * @param testStartTimeMs      epoch millis of test start
     * @param testEndTimeMs        epoch millis of test end
     * @return the filename and saved path of the generated ZIP
     */
    public PackageResult packageResults(
            PerfTestResult result,
            List<String> dashboardImageFiles,
            String prometheusExportFile,
            List<LogEntry> logEntries,
            String testId,
            long testStartTimeMs,
            long testEndTimeMs) {

        var timestamp = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")
                .format(Instant.now().atZone(java.time.ZoneId.systemDefault()));
        var packageName = testId != null ? testId + "_" + timestamp : "perf_test_" + timestamp;
        var filename = packageName + ".zip";

        var exportDir = Path.of(exportPath);
        try {
            Files.createDirectories(exportDir);
        } catch (IOException e) {
            log.error("Failed to create export directory: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to create export directory", e);
        }

        var zipPath = exportDir.resolve(filename);

        // Write ZIP directly to disk to avoid OOM with large exports
        try (ZipOutputStream zos = new ZipOutputStream(new BufferedOutputStream(Files.newOutputStream(zipPath)))) {

            // Add test summary/statistics file
            var summaryContent = generateSummary(result, logEntries, testId, testStartTimeMs, testEndTimeMs);
            addTextEntry(zos, "summary.txt", summaryContent);

            // Add dashboard images - stream from disk
            for (String imageFile : dashboardImageFiles) {
                if (imageFile != null) {
                    addFileEntryStreaming(zos, Path.of(imageFile), "dashboards/");
                }
            }

            // Add Prometheus export - stream from disk
            if (prometheusExportFile != null) {
                addFileEntryStreaming(zos, Path.of(prometheusExportFile), "metrics/");
            }

            // Add application logs from Loki
            if (logEntries != null && !logEntries.isEmpty()) {
                var logContent = formatLogEntries(logEntries);
                addTextEntry(zos, "logs/application.log", logContent);
            }

            // Add database export query results as CSV files
            if (result.dbQueryResults() != null && !result.dbQueryResults().isEmpty()) {
                for (var entry : result.dbQueryResults().entrySet()) {
                    var csvFilename = sanitizeFilename(entry.getKey()) + ".csv";
                    addTextEntry(zos, "db/" + csvFilename, entry.getValue());
                }
            }

            // Add Kubernetes cluster info - stream from disk
            if (result.kubernetesExportFile() != null) {
                var kubernetesPath = Path.of(result.kubernetesExportFile());
                if (Files.isDirectory(kubernetesPath)) {
                    try (var kubernetesFiles = Files.list(kubernetesPath)) {
                        kubernetesFiles.filter(Files::isRegularFile)
                                .sorted()
                                .forEach(file -> {
                                    try {
                                        addFileEntryStreaming(zos, file, "kubernetes/");
                                    } catch (IOException ex) {
                                        log.warn("Failed to add kubernetes file {}: {}", file, ex.getMessage());
                                    }
                                });
                    }
                } else {
                    addFileEntryStreaming(zos, kubernetesPath, "kubernetes/");
                }
            }

        } catch (IOException e) {
            log.error("Failed to package test results: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to package test results", e);
        }

        log.info("Test results packaged to: {}", zipPath.toAbsolutePath());

        // Return path-based result - ZIP is streamed from disk via FileSystemResource
        return new PackageResult(filename, zipPath.toAbsolutePath().toString());
    }

    private String generateSummary(PerfTestResult result, List<LogEntry> logEntries,
                                   String testId, long testStartTimeMs, long testEndTimeMs) {
        var sb = new StringBuilder();
        sb.append("═══════════════════════════════════════════════════════════════\n");
        sb.append("                    PERFORMANCE TEST SUMMARY\n");
        sb.append("═══════════════════════════════════════════════════════════════\n\n");

        if (testId != null) {
            sb.append(String.format("Test ID:              %s%n", testId));
        }
        sb.append(String.format("Test Start:           %s%n", Instant.ofEpochMilli(testStartTimeMs)));
        sb.append(String.format("Test End:             %s%n", Instant.ofEpochMilli(testEndTimeMs)));
        sb.append("\n");

        sb.append("───────────────────────────────────────────────────────────────\n");
        sb.append("                         THROUGHPUT\n");
        sb.append("───────────────────────────────────────────────────────────────\n");
        sb.append(String.format("Completed Messages:   %d%n", result.completedMessages()));
        sb.append(String.format("Pending Messages:     %d%n", result.pendingMessages()));
        sb.append(String.format("Test Duration:        %.2f seconds%n", result.testDurationSeconds()));
        sb.append(String.format("Throughput (TPS):     %.2f msg/sec%n", result.tps()));
        sb.append("\n");

        sb.append("───────────────────────────────────────────────────────────────\n");
        sb.append("                          LATENCY\n");
        sb.append("───────────────────────────────────────────────────────────────\n");
        sb.append(String.format("Average Latency:      %.2f ms%n", result.avgLatencyMs()));
        sb.append(String.format("Min Latency:          %.2f ms%n", result.minLatencyMs()));
        sb.append(String.format("Max Latency:          %.2f ms%n", result.maxLatencyMs()));
        sb.append("\n");

        sb.append("───────────────────────────────────────────────────────────────\n");
        sb.append("                         ARTIFACTS\n");
        sb.append("───────────────────────────────────────────────────────────────\n");
        sb.append("Dashboard URLs:\n");
        if (result.dashboardUrls() != null) {
            for (var url : result.dashboardUrls()) {
                sb.append(String.format("  • %s%n", url));
            }
        }
        sb.append("\n");

        sb.append("Exported Files:\n");
        sb.append("  • summary.txt (this file)\n");
        if (result.dashboardExportFiles() != null) {
            for (var file : result.dashboardExportFiles()) {
                sb.append(String.format("  • dashboards/%s%n", Path.of(file).getFileName()));
            }
        }
        if (result.prometheusExportFile() != null) {
            sb.append(String.format("  • metrics/%s%n", Path.of(result.prometheusExportFile()).getFileName()));
        }
        if (result.kubernetesExportFile() != null) {
            var kubernetesPath = Path.of(result.kubernetesExportFile());
            if (Files.isDirectory(kubernetesPath)) {
                try (var kubernetesFiles = Files.list(kubernetesPath)) {
                    kubernetesFiles.filter(Files::isRegularFile)
                            .sorted()
                            .forEach(file -> sb.append(String.format("  • kubernetes/%s%n", file.getFileName())));
                } catch (IOException e) {
                    sb.append("  • kubernetes/ (failed to list files)\n");
                }
            } else {
                sb.append(String.format("  • kubernetes/%s%n", kubernetesPath.getFileName()));
            }
        }
        if (logEntries != null && !logEntries.isEmpty()) {
            sb.append(String.format("  • logs/application.log (%d entries)%n", logEntries.size()));
        }
        if (result.dbQueryResults() != null && !result.dbQueryResults().isEmpty()) {
            for (var entry : result.dbQueryResults().entrySet()) {
                sb.append(String.format("  • db/%s.csv%n", sanitizeFilename(entry.getKey())));
            }
        }

        sb.append("\n═══════════════════════════════════════════════════════════════\n");
        sb.append(String.format("Generated: %s%n", Instant.now()));

        return sb.toString();
    }

    private String formatLogEntries(List<LogEntry> logEntries) {
        var sb = new StringBuilder();
        for (var entry : logEntries) {
            sb.append(String.format("%-30s %-5s %s%n", entry.timestamp(), entry.level(), entry.message()));
        }
        return sb.toString();
    }

    private void addTextEntry(ZipOutputStream zos, String entryName, String content) throws IOException {
        var entry = new ZipEntry(entryName);
        zos.putNextEntry(entry);
        zos.write(content.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        zos.closeEntry();
    }

    private String sanitizeFilename(String name) {
        return name.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private void addFileEntryStreaming(ZipOutputStream zos, Path file, String prefix) throws IOException {
        if (Files.exists(file)) {
            var entryName = prefix + file.getFileName().toString();
            var entry = new ZipEntry(entryName);
            zos.putNextEntry(entry);
            // Stream file content to avoid loading entire file into memory
            try (InputStream is = Files.newInputStream(file)) {
                is.transferTo(zos);
            }
            zos.closeEntry();
            log.debug("Added to ZIP (streamed): {}", entryName);
        } else {
            log.warn("File not found, skipping: {}", file);
        }
    }

    public record PackageResult(String filename, String savedPath) {}
}
