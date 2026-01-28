package com.example.perftester.export;

import com.example.perftester.perf.PerfTestResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.junit.jupiter.api.Assertions.assertNotNull;
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

        assertNotNull(packageResult.zipBytes());
        assertTrue(packageResult.zipBytes().length > 0);
        assertTrue(packageResult.filename().startsWith("test-id_"));
        assertTrue(packageResult.filename().endsWith(".zip"));
        assertTrue(Files.exists(Path.of(packageResult.savedPath())));
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

        assertTrue(zipContainsEntry(packageResult.zipBytes(), "summary.txt"));
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

        assertTrue(zipContainsEntry(packageResult.zipBytes(), "dashboards/dashboard.png"));
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

        assertTrue(zipContainsEntry(packageResult.zipBytes(), "metrics/prometheus.json"));
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

        assertNotNull(packageResult.zipBytes());
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
        assertNotNull(packageResult.zipBytes());
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
        byte[] zipBytes = new byte[]{1, 2, 3};
        String filename = "test.zip";
        String savedPath = "/path/to/test.zip";

        TestResultPackager.PackageResult result = new TestResultPackager.PackageResult(zipBytes, filename, savedPath);

        assertNotNull(result.zipBytes());
        assertTrue(result.filename().equals(filename));
        assertTrue(result.savedPath().equals(savedPath));
    }

    private boolean zipContainsEntry(byte[] zipBytes, String entryName) throws IOException {
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
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
