package com.example.perftester.perf;

import com.example.perftester.kubernetes.KubernetesNodeInfo;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PerfTestResultTest {

    @Test
    void compactConstructorShouldSetDefaults() {
        PerfTestResult result = new PerfTestResult(100, 5, 10.5, 9.52, 50.0, 10.0, 200.0);

        assertEquals(100, result.completedMessages());
        assertEquals(5, result.pendingMessages());
        assertEquals(10.5, result.testDurationSeconds());
        assertEquals(9.52, result.tps());
        assertEquals(50.0, result.avgLatencyMs());
        assertEquals(10.0, result.minLatencyMs());
        assertEquals(200.0, result.maxLatencyMs());
        assertEquals(List.of(), result.dashboardUrls());
        assertEquals(List.of(), result.dashboardExportFiles());
        assertNull(result.prometheusExportFile());
        assertEquals(List.of(), result.kubernetesNodes());
    }

    @Test
    void fullConstructorShouldSetAllFields() {
        List<String> urls = List.of("http://grafana/d/1", "http://grafana/d/2");
        List<String> files = List.of("/path/to/file1.png", "/path/to/file2.png");
        String prometheusFile = "/path/to/prometheus.json";
        List<KubernetesNodeInfo> nodes = List.of(createSampleNodeInfo());

        PerfTestResult result = new PerfTestResult(
                100, 5, 10.5, 9.52, 50.0, 10.0, 200.0,
                urls, files, prometheusFile, nodes
        );

        assertEquals(urls, result.dashboardUrls());
        assertEquals(files, result.dashboardExportFiles());
        assertEquals(prometheusFile, result.prometheusExportFile());
        assertEquals(nodes, result.kubernetesNodes());
    }

    @Test
    void withDashboardExportsShouldReturnNewInstanceWithUrls() {
        PerfTestResult original = new PerfTestResult(100, 5, 10.5, 9.52, 50.0, 10.0, 200.0);
        List<String> urls = List.of("http://grafana/d/test");
        List<String> files = List.of("/path/to/export.png");

        PerfTestResult updated = original.withDashboardExports(urls, files);

        // Original should be unchanged
        assertEquals(List.of(), original.dashboardUrls());
        assertEquals(List.of(), original.dashboardExportFiles());

        // New instance should have the values
        assertEquals(urls, updated.dashboardUrls());
        assertEquals(files, updated.dashboardExportFiles());

        // Other fields should be preserved
        assertEquals(original.completedMessages(), updated.completedMessages());
        assertEquals(original.tps(), updated.tps());
    }

    @Test
    void withPrometheusExportShouldReturnNewInstanceWithFile() {
        List<String> urls = List.of("http://grafana/d/test");
        List<String> files = List.of("/path/to/export.png");
        PerfTestResult original = new PerfTestResult(
                100, 5, 10.5, 9.52, 50.0, 10.0, 200.0,
                urls, files, null, List.of()
        );

        String prometheusFile = "/path/to/prometheus.json";
        PerfTestResult updated = original.withPrometheusExport(prometheusFile);

        // Original should be unchanged
        assertNull(original.prometheusExportFile());

        // New instance should have the file
        assertEquals(prometheusFile, updated.prometheusExportFile());

        // Dashboard exports should be preserved
        assertEquals(urls, updated.dashboardUrls());
        assertEquals(files, updated.dashboardExportFiles());
    }

    @Test
    void withKubernetesNodesShouldReturnNewInstanceWithNodes() {
        PerfTestResult original = new PerfTestResult(100, 5, 10.5, 9.52, 50.0, 10.0, 200.0);
        List<KubernetesNodeInfo> nodes = List.of(createSampleNodeInfo());

        PerfTestResult updated = original.withKubernetesNodes(nodes);

        // Original should be unchanged
        assertTrue(original.kubernetesNodes().isEmpty());

        // New instance should have the nodes
        assertEquals(nodes, updated.kubernetesNodes());

        // Other fields should be preserved
        assertEquals(original.completedMessages(), updated.completedMessages());
        assertEquals(original.tps(), updated.tps());
    }

    private KubernetesNodeInfo createSampleNodeInfo() {
        return new KubernetesNodeInfo(
                "node-1",
                "v1.28.0",
                "Ubuntu 22.04",
                "amd64",
                "containerd://1.6.20",
                "4",
                "3800m",
                "16Gi",
                "15Gi",
                "100Gi",
                "90Gi",
                "110",
                List.of("Ready=True", "MemoryPressure=False")
        );
    }

    @Test
    void chainedWithMethodsShouldWork() {
        PerfTestResult result = new PerfTestResult(100, 0, 10.0, 10.0, 50.0, 10.0, 100.0)
                .withDashboardExports(List.of("url1"), List.of("file1"))
                .withPrometheusExport("prometheus.json");

        assertEquals(List.of("url1"), result.dashboardUrls());
        assertEquals(List.of("file1"), result.dashboardExportFiles());
        assertEquals("prometheus.json", result.prometheusExportFile());
    }
}
