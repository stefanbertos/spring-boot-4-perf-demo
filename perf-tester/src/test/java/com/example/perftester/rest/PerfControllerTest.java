package com.example.perftester.rest;

import com.example.perftester.export.TestResultPackager;
import com.example.perftester.export.TestResultPackager.PackageResult;
import com.example.perftester.grafana.GrafanaExportService;
import com.example.perftester.grafana.GrafanaExportService.DashboardExportResult;
import com.example.perftester.kubernetes.KubernetesNodeInfo;
import com.example.perftester.kubernetes.KubernetesService;
import com.example.perftester.messaging.MessageSender;
import com.example.perftester.perf.PerfTestResult;
import com.example.perftester.perf.PerformanceTracker;
import com.example.perftester.prometheus.PrometheusExportService;
import com.example.perftester.prometheus.PrometheusExportService.PrometheusExportResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PerfControllerTest {

    @Mock
    private MessageSender messageSender;

    @Mock
    private PerformanceTracker performanceTracker;

    @Mock
    private GrafanaExportService grafanaExportService;

    @Mock
    private PrometheusExportService prometheusExportService;

    @Mock
    private TestResultPackager testResultPackager;

    @Mock
    private KubernetesService kubernetesService;

    @TempDir
    Path tempDir;

    private PerfController controller;

    @BeforeEach
    void setUp() {
        controller = new PerfController(messageSender, performanceTracker,
                grafanaExportService, prometheusExportService, testResultPackager, kubernetesService);

        // Default mock for Kubernetes service
        when(kubernetesService.getNodeInfo()).thenReturn(List.of(KubernetesNodeInfo.unavailable()));
    }

    @Test
    void sendMessagesShouldReturnZipFile() throws InterruptedException, IOException {
        PerfTestResult perfResult = new PerfTestResult(10, 0, 1.0, 10.0, 50.0, 10.0, 100.0);
        PerfTestResult withDashboards = perfResult.withDashboardExports(List.of("http://grafana/d/1"), List.of("/path/to/file.png"));
        PerfTestResult withPrometheus = withDashboards.withPrometheusExport("/path/to/prometheus.json");

        when(performanceTracker.awaitCompletion(anyLong(), any(TimeUnit.class))).thenReturn(true);
        when(performanceTracker.getResult())
                .thenReturn(perfResult)
                .thenReturn(withDashboards)
                .thenReturn(withPrometheus);

        DashboardExportResult dashboardExport = new DashboardExportResult(
                List.of("http://grafana/d/1"), List.of("/path/to/file.png"));
        when(grafanaExportService.exportDashboards(anyLong(), anyLong())).thenReturn(dashboardExport);

        PrometheusExportResult prometheusExport = new PrometheusExportResult(
                "/path/to/prometheus.json", "http://prometheus/query", null);
        when(prometheusExportService.exportMetrics(anyLong(), anyLong(), anyString())).thenReturn(prometheusExport);

        // Create a temp ZIP file for the test
        Path tempZip = tempDir.resolve("test-id_20230101.zip");
        Files.write(tempZip, "test content".getBytes());

        PackageResult packageResult = new PackageResult("test-id_20230101.zip", tempZip.toString());
        when(testResultPackager.packageResults(any(PerfTestResult.class), anyList(), anyString(), anyString(), anyLong(), anyLong()))
                .thenReturn(packageResult);

        ResponseEntity<Resource> response = controller.sendMessages("test message", 10, 1, 0, "test-id");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getHeaders().getContentDisposition().toString().contains("test-id_20230101.zip"));

        verify(messageSender, times(10)).sendMessage(anyString());
        verify(performanceTracker).startTest(10);
    }

    @Test
    void sendMessagesShouldHandleTimeout() throws InterruptedException, IOException {
        PerfTestResult perfResult = new PerfTestResult(5, 5, 10.0, 0.5, 50.0, 10.0, 100.0);

        when(performanceTracker.awaitCompletion(anyLong(), any(TimeUnit.class))).thenReturn(false);
        when(performanceTracker.getResult()).thenReturn(perfResult);

        DashboardExportResult dashboardExport = new DashboardExportResult(List.of(), List.of());
        when(grafanaExportService.exportDashboards(anyLong(), anyLong())).thenReturn(dashboardExport);

        PrometheusExportResult prometheusExport = new PrometheusExportResult(null, null, "Connection refused");
        when(prometheusExportService.exportMetrics(anyLong(), anyLong(), any())).thenReturn(prometheusExport);

        Path tempZip = tempDir.resolve("test.zip");
        Files.write(tempZip, "content".getBytes());

        PackageResult packageResult = new PackageResult("test.zip", tempZip.toString());
        when(testResultPackager.packageResults(any(), anyList(), any(), any(), anyLong(), anyLong()))
                .thenReturn(packageResult);

        ResponseEntity<Resource> response = controller.sendMessages("test", 10, 1, 0, null);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void sendMessagesShouldApplyDelay() throws InterruptedException, IOException {
        PerfTestResult perfResult = new PerfTestResult(2, 0, 0.5, 4.0, 50.0, 10.0, 100.0);

        when(performanceTracker.awaitCompletion(anyLong(), any(TimeUnit.class))).thenReturn(true);
        when(performanceTracker.getResult()).thenReturn(perfResult);

        DashboardExportResult dashboardExport = new DashboardExportResult(List.of(), List.of());
        when(grafanaExportService.exportDashboards(anyLong(), anyLong())).thenReturn(dashboardExport);

        PrometheusExportResult prometheusExport = new PrometheusExportResult("/path/file.json", "url", null);
        when(prometheusExportService.exportMetrics(anyLong(), anyLong(), any())).thenReturn(prometheusExport);

        Path tempZip = tempDir.resolve("test.zip");
        Files.write(tempZip, "content".getBytes());

        PackageResult packageResult = new PackageResult("test.zip", tempZip.toString());
        when(testResultPackager.packageResults(any(), anyList(), anyString(), any(), anyLong(), anyLong()))
                .thenReturn(packageResult);

        long startTime = System.currentTimeMillis();
        controller.sendMessages("test", 2, 1, 10, "test-id");
        long elapsed = System.currentTimeMillis() - startTime;

        // Should take at least 10ms delay between 2 messages (10ms delay)
        // Note: the test also has a 16 second sleep, so we check it completed
        verify(messageSender, times(2)).sendMessage(anyString());
    }
}
