package com.example.perftester.export;

import com.example.perftester.perf.PerfTestResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
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

    private final ObjectMapper objectMapper;
    private final String exportPath;

    public TestResultPackager(@Value("${app.export.path:./test-exports}") String exportPath) {
        this.objectMapper = new ObjectMapper()
                .enable(SerializationFeature.INDENT_OUTPUT);
        this.exportPath = exportPath;
    }

    public PackageResult packageResults(
            PerfTestResult result,
            List<String> dashboardImageFiles,
            String prometheusExportFile,
            String testId,
            long testStartTimeMs,
            long testEndTimeMs) {

        String timestamp = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")
                .format(Instant.now().atZone(java.time.ZoneId.systemDefault()));
        String packageName = testId != null ? testId + "_" + timestamp : "perf_test_" + timestamp;

        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (ZipOutputStream zos = new ZipOutputStream(baos)) {

                // Add test summary/statistics file
                String summaryContent = generateSummary(result, testId, testStartTimeMs, testEndTimeMs);
                addTextEntry(zos, "summary.txt", summaryContent);

                // Add dashboard images
                for (String imageFile : dashboardImageFiles) {
                    if (imageFile != null) {
                        addFileEntry(zos, Path.of(imageFile), "dashboards/");
                    }
                }

                // Add Prometheus export
                if (prometheusExportFile != null) {
                    addFileEntry(zos, Path.of(prometheusExportFile), "metrics/");
                }
            }

            byte[] zipBytes = baos.toByteArray();
            String filename = packageName + ".zip";

            // Also save to disk
            Path exportDir = Path.of(exportPath);
            Files.createDirectories(exportDir);
            Path zipPath = exportDir.resolve(filename);
            Files.write(zipPath, zipBytes);
            log.info("Test results packaged to: {}", zipPath.toAbsolutePath());

            return new PackageResult(zipBytes, filename, zipPath.toAbsolutePath().toString());

        } catch (IOException e) {
            log.error("Failed to package test results: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to package test results", e);
        }
    }

    private String generateSummary(PerfTestResult result, String testId, long testStartTimeMs, long testEndTimeMs) {
        StringBuilder sb = new StringBuilder();
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
            for (String url : result.dashboardUrls()) {
                sb.append(String.format("  • %s%n", url));
            }
        }
        sb.append("\n");

        sb.append("Exported Files:\n");
        sb.append("  • summary.txt (this file)\n");
        sb.append("  • result.json\n");
        if (result.dashboardExportFiles() != null) {
            for (String file : result.dashboardExportFiles()) {
                sb.append(String.format("  • dashboards/%s%n", Path.of(file).getFileName()));
            }
        }
        if (result.prometheusExportFile() != null) {
            sb.append(String.format("  • metrics/%s%n", Path.of(result.prometheusExportFile()).getFileName()));
        }

        sb.append("\n═══════════════════════════════════════════════════════════════\n");
        sb.append(String.format("Generated: %s%n", Instant.now()));

        return sb.toString();
    }

    private void addTextEntry(ZipOutputStream zos, String entryName, String content) throws IOException {
        ZipEntry entry = new ZipEntry(entryName);
        zos.putNextEntry(entry);
        zos.write(content.getBytes());
        zos.closeEntry();
    }

    private void addFileEntry(ZipOutputStream zos, Path file, String prefix) throws IOException {
        if (Files.exists(file)) {
            String entryName = prefix + file.getFileName().toString();
            ZipEntry entry = new ZipEntry(entryName);
            zos.putNextEntry(entry);
            zos.write(Files.readAllBytes(file));
            zos.closeEntry();
            log.debug("Added to ZIP: {}", entryName);
        } else {
            log.warn("File not found, skipping: {}", file);
        }
    }

    public record PackageResult(byte[] zipBytes, String filename, String savedPath) {}
}
