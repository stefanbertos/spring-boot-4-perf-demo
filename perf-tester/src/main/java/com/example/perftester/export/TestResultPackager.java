package com.example.perftester.export;

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

    public PackageResult packageResults(
            PerfTestResult result,
            List<String> dashboardImageFiles,
            String prometheusExportFile,
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
            var summaryContent = generateSummary(result, testId, testStartTimeMs, testEndTimeMs);
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

            // Add Kubernetes node info - stream from disk
            if (result.kubernetesExportFile() != null) {
                addFileEntryStreaming(zos, Path.of(result.kubernetesExportFile()), "kubernetes/");
            }

        } catch (IOException e) {
            log.error("Failed to package test results: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to package test results", e);
        }

        log.info("Test results packaged to: {}", zipPath.toAbsolutePath());

        // Return path-based result - ZIP is streamed from disk via FileSystemResource
        return new PackageResult(filename, zipPath.toAbsolutePath().toString());
    }

    private String generateSummary(PerfTestResult result, String testId, long testStartTimeMs, long testEndTimeMs) {
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
        sb.append("  • result.json\n");
        if (result.dashboardExportFiles() != null) {
            for (var file : result.dashboardExportFiles()) {
                sb.append(String.format("  • dashboards/%s%n", Path.of(file).getFileName()));
            }
        }
        if (result.prometheusExportFile() != null) {
            sb.append(String.format("  • metrics/%s%n", Path.of(result.prometheusExportFile()).getFileName()));
        }
        if (result.kubernetesExportFile() != null) {
            sb.append(String.format("  • kubernetes/%s%n", Path.of(result.kubernetesExportFile()).getFileName()));
        }

        sb.append("\n═══════════════════════════════════════════════════════════════\n");
        sb.append(String.format("Generated: %s%n", Instant.now()));

        return sb.toString();
    }

    private void addTextEntry(ZipOutputStream zos, String entryName, String content) throws IOException {
        var entry = new ZipEntry(entryName);
        zos.putNextEntry(entry);
        zos.write(content.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        zos.closeEntry();
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
