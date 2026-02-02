package com.example.perftester.export;

import com.example.perftester.perf.PerfTestResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TestResultPackagerTest {

    @TempDir
    Path tempDir;

    private TestResultPackager packager;

    @BeforeEach
    void setUp() {
        packager = new TestResultPackager(tempDir.toString());
    }

    @Test
    void packageResultsShouldCreateZipFile() throws IOException {
        PerfTestResult result = new PerfTestResult(100, 0, 10.0, 10.0, 50.0, 10.0, 100.0);

        TestResultPackager.PackageResult packageResult = packager.packageResults(
                result,
                List.of(),
                null,
                "test-id",
                System.currentTimeMillis() - 10000,
                System.currentTimeMillis()
        );

        assertNotNull(packageResult.savedPath());
        assertTrue(Files.exists(Path.of(packageResult.savedPath())));
        assertTrue(Files.size(Path.of(packageResult.savedPath())) > 0);
        assertTrue(packageResult.filename().startsWith("test-id_"));
        assertTrue(packageResult.filename().endsWith(".zip"));
    }

    @Test
    void packageResultsShouldContainSummaryFile() throws IOException {
        PerfTestResult result = new PerfTestResult(100, 5, 10.0, 10.0, 50.0, 10.0, 100.0);

        TestResultPackager.PackageResult packageResult = packager.packageResults(
                result,
                List.of(),
                null,
                null,
                System.currentTimeMillis() - 10000,
                System.currentTimeMillis()
        );

        assertTrue(zipContainsEntry(packageResult.savedPath(), "summary.txt"));
    }

    @Test
    void packageResultsShouldIncludeDashboardFiles() throws IOException {
        // Create a temp dashboard file
        Path dashboardFile = tempDir.resolve("dashboard.png");
        Files.write(dashboardFile, "fake image data".getBytes());

        PerfTestResult result = new PerfTestResult(100, 0, 10.0, 10.0, 50.0, 10.0, 100.0)
                .withDashboardExports(List.of("http://grafana/d/1"), List.of(dashboardFile.toString()));

        TestResultPackager.PackageResult packageResult = packager.packageResults(
                result,
                List.of(dashboardFile.toString()),
                null,
                "test",
                System.currentTimeMillis(),
                System.currentTimeMillis()
        );

        assertTrue(zipContainsEntry(packageResult.savedPath(), "dashboards/dashboard.png"));
    }

    @Test
    void packageResultsShouldIncludePrometheusFile() throws IOException {
        // Create a temp prometheus file
        Path prometheusFile = tempDir.resolve("prometheus.json");
        Files.write(prometheusFile, "{\"metrics\": []}".getBytes());

        PerfTestResult result = new PerfTestResult(100, 0, 10.0, 10.0, 50.0, 10.0, 100.0)
                .withPrometheusExport(prometheusFile.toString());

        TestResultPackager.PackageResult packageResult = packager.packageResults(
                result,
                List.of(),
                prometheusFile.toString(),
                "test",
                System.currentTimeMillis(),
                System.currentTimeMillis()
        );

        assertTrue(zipContainsEntry(packageResult.savedPath(), "metrics/prometheus.json"));
    }

    @Test
    void packageResultsShouldHandleNullDashboardFiles() throws IOException {
        PerfTestResult result = new PerfTestResult(100, 0, 10.0, 10.0, 50.0, 10.0, 100.0);

        // Use ArrayList which allows null elements (unlike List.of())
        List<String> filesWithNull = new ArrayList<>();
        filesWithNull.add(null);

        TestResultPackager.PackageResult packageResult = packager.packageResults(
                result,
                filesWithNull,
                null,
                "test",
                System.currentTimeMillis(),
                System.currentTimeMillis()
        );

        assertNotNull(packageResult.savedPath());
        assertTrue(Files.exists(Path.of(packageResult.savedPath())));
    }

    @Test
    void packageResultsShouldHandleMissingFiles() throws IOException {
        PerfTestResult result = new PerfTestResult(100, 0, 10.0, 10.0, 50.0, 10.0, 100.0);

        TestResultPackager.PackageResult packageResult = packager.packageResults(
                result,
                List.of("/nonexistent/file.png"),
                "/nonexistent/prometheus.json",
                "test",
                System.currentTimeMillis(),
                System.currentTimeMillis()
        );

        // Should not throw, just skip missing files
        assertNotNull(packageResult.savedPath());
        assertTrue(Files.exists(Path.of(packageResult.savedPath())));
    }

    @Test
    void packageResultsShouldGenerateTimestampedFilename() throws IOException {
        PerfTestResult result = new PerfTestResult(100, 0, 10.0, 10.0, 50.0, 10.0, 100.0);

        TestResultPackager.PackageResult packageResult = packager.packageResults(
                result,
                List.of(),
                null,
                null, // No test ID
                System.currentTimeMillis(),
                System.currentTimeMillis()
        );

        assertTrue(packageResult.filename().startsWith("perf_test_"));
    }

    @Test
    void packageResultRecordShouldStoreValues() {
        String filename = "test.zip";
        String savedPath = "/path/to/test.zip";

        TestResultPackager.PackageResult result = new TestResultPackager.PackageResult(filename, savedPath);

        assertTrue(result.filename().equals(filename));
        assertTrue(result.savedPath().equals(savedPath));
    }

    @Test
    void packageResultsShouldIncludeDashboardUrlsInSummary() throws IOException {
        PerfTestResult result = new PerfTestResult(100, 0, 10.0, 10.0, 50.0, 10.0, 100.0)
                .withDashboardExports(List.of("http://grafana/d/1", "http://grafana/d/2"), List.of());

        TestResultPackager.PackageResult packageResult = packager.packageResults(
                result,
                List.of(),
                null,
                "test",
                System.currentTimeMillis() - 10000,
                System.currentTimeMillis()
        );

        assertTrue(zipContainsEntry(packageResult.savedPath(), "summary.txt"));
        String summaryContent = getZipEntryContent(packageResult.savedPath(), "summary.txt");
        assertTrue(summaryContent.contains("http://grafana/d/1"));
        assertTrue(summaryContent.contains("http://grafana/d/2"));
    }

    @Test
    void packageResultsShouldIncludeDashboardFilesInSummary() throws IOException {
        Path dashboardFile = tempDir.resolve("dashboard.png");
        Files.write(dashboardFile, "fake image data".getBytes());

        PerfTestResult result = new PerfTestResult(100, 0, 10.0, 10.0, 50.0, 10.0, 100.0)
                .withDashboardExports(List.of(), List.of(dashboardFile.toString()));

        TestResultPackager.PackageResult packageResult = packager.packageResults(
                result,
                List.of(dashboardFile.toString()),
                null,
                "test",
                System.currentTimeMillis() - 10000,
                System.currentTimeMillis()
        );

        String summaryContent = getZipEntryContent(packageResult.savedPath(), "summary.txt");
        assertTrue(summaryContent.contains("dashboard.png"));
    }

    @Test
    void packageResultsShouldIncludePrometheusFileInSummary() throws IOException {
        Path prometheusFile = tempDir.resolve("prometheus.json");
        Files.write(prometheusFile, "{\"metrics\": []}".getBytes());

        PerfTestResult result = new PerfTestResult(100, 0, 10.0, 10.0, 50.0, 10.0, 100.0)
                .withPrometheusExport(prometheusFile.toString());

        TestResultPackager.PackageResult packageResult = packager.packageResults(
                result,
                List.of(),
                prometheusFile.toString(),
                "test",
                System.currentTimeMillis() - 10000,
                System.currentTimeMillis()
        );

        String summaryContent = getZipEntryContent(packageResult.savedPath(), "summary.txt");
        assertTrue(summaryContent.contains("prometheus.json"));
    }

    @Test
    void packageResultsShouldIncludeTestIdInSummary() throws IOException {
        PerfTestResult result = new PerfTestResult(100, 0, 10.0, 10.0, 50.0, 10.0, 100.0);

        TestResultPackager.PackageResult packageResult = packager.packageResults(
                result,
                List.of(),
                null,
                "my-test-id",
                System.currentTimeMillis() - 10000,
                System.currentTimeMillis()
        );

        String summaryContent = getZipEntryContent(packageResult.savedPath(), "summary.txt");
        assertTrue(summaryContent.contains("my-test-id"));
    }

    @Test
    void packageResultsShouldHandleMultipleDashboardFiles() throws IOException {
        Path file1 = tempDir.resolve("dashboard1.png");
        Path file2 = tempDir.resolve("dashboard2.png");
        Files.write(file1, "data1".getBytes());
        Files.write(file2, "data2".getBytes());

        PerfTestResult result = new PerfTestResult(100, 0, 10.0, 10.0, 50.0, 10.0, 100.0);

        TestResultPackager.PackageResult packageResult = packager.packageResults(
                result,
                List.of(file1.toString(), file2.toString()),
                null,
                "test",
                System.currentTimeMillis(),
                System.currentTimeMillis()
        );

        assertTrue(zipContainsEntry(packageResult.savedPath(), "dashboards/dashboard1.png"));
        assertTrue(zipContainsEntry(packageResult.savedPath(), "dashboards/dashboard2.png"));
    }

    @Test
    void packageResultsShouldThrowWhenDirectoryCreationFails() throws Exception {
        // Create a file where the directory should be created
        Path blockingFile = tempDir.resolve("blocking-file");
        Files.write(blockingFile, "blocking content".getBytes());

        // Try to create subdirectory inside a file (should fail)
        TestResultPackager invalidPathPackager = new TestResultPackager(blockingFile.resolve("subdir").toString());
        PerfTestResult result = new PerfTestResult(100, 0, 10.0, 10.0, 50.0, 10.0, 100.0);

        assertThrows(RuntimeException.class, () -> invalidPathPackager.packageResults(
                result,
                List.of(),
                null,
                "test",
                System.currentTimeMillis(),
                System.currentTimeMillis()
        ));
    }

    private String getZipEntryContent(String zipPath, String entryName) throws IOException {
        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(Path.of(zipPath)))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.getName().equals(entryName)) {
                    return new String(zis.readAllBytes());
                }
            }
        }
        return "";
    }

    private boolean zipContainsEntry(String zipPath, String entryName) throws IOException {
        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(Path.of(zipPath)))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.getName().equals(entryName)) {
                    return true;
                }
            }
        }
        return false;
    }
}
