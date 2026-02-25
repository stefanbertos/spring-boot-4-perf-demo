package com.example.perftester.prometheus;

import com.example.perftester.config.PerfProperties;
import com.example.perftester.prometheus.PrometheusProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClient;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PrometheusExportServiceTest {

    @TempDir
    Path tempDir;

    @Mock
    private RestClient restClient;

    private PrometheusExportService service;

    @BeforeEach
    void setUp() {
        service = new PrometheusExportService(
                new PrometheusProperties("http://localhost:9090", tempDir.toString()),
                new PerfProperties(16000, 60000, 60000, 30000, 60, 15)
        );
        // Inject mock RestClient
        ReflectionTestUtils.setField(service, "restClient", restClient);
    }

    @Test
    void exportMetricsShouldCreateJsonFile() {
        setupRestClientMock(
                """
                {"status":"success","data":["metric1","metric2"]}
                """,
                """
                {"status":"success","data":{"resultType":"matrix","result":[{"metric":{"__name__":"metric1"},"values":[[1234567890,"1.0"]]}]}}
                """
        );

        long startTime = System.currentTimeMillis() - 10000;
        long endTime = System.currentTimeMillis();

        PrometheusExportService.PrometheusExportResult result = service.exportMetrics(startTime, endTime, "test-id");

        assertTrue(result.isSuccess());
        assertNotNull(result.filePath());
        assertNull(result.error());
        assertTrue(Files.exists(Path.of(result.filePath())));
    }

    @Test
    void exportMetricsShouldReturnQueryUrl() {
        setupRestClientMock(
                """
                {"status":"success","data":["metric1"]}
                """,
                """
                {"status":"success","data":{"resultType":"matrix","result":[]}}
                """
        );

        long startTime = System.currentTimeMillis() - 10000;
        long endTime = System.currentTimeMillis();

        PrometheusExportService.PrometheusExportResult result = service.exportMetrics(startTime, endTime, "test-id");

        assertNotNull(result.queryUrl());
        assertTrue(result.queryUrl().contains("/api/v1/query_range"));
    }

    @Test
    void exportMetricsShouldHandleEmptyMetricsList() {
        setupRestClientMock(
                """
                {"status":"success","data":[]}
                """,
                null
        );

        long startTime = System.currentTimeMillis() - 10000;
        long endTime = System.currentTimeMillis();

        PrometheusExportService.PrometheusExportResult result = service.exportMetrics(startTime, endTime, null);

        assertTrue(result.isSuccess());
        assertNotNull(result.filePath());
    }

    @Test
    void exportMetricsShouldHandleFailedMetricFetch() {
        setupRestClientMock(
                """
                {"status":"error","error":"some error"}
                """,
                null
        );

        long startTime = System.currentTimeMillis() - 10000;
        long endTime = System.currentTimeMillis();

        PrometheusExportService.PrometheusExportResult result = service.exportMetrics(startTime, endTime, "test");

        assertTrue(result.isSuccess());
    }

    @Test
    void exportMetricsShouldHandleRestClientException() {
        when(restClient.get()).thenThrow(new RuntimeException("Connection refused"));

        long startTime = System.currentTimeMillis() - 10000;
        long endTime = System.currentTimeMillis();

        PrometheusExportService.PrometheusExportResult result = service.exportMetrics(startTime, endTime, "test");

        assertTrue(result.isSuccess());
    }

    @Test
    void exportMetricsShouldSkipEmptyResults() {
        setupRestClientMock(
                """
                {"status":"success","data":["metric1","metric2"]}
                """,
                """
                {"status":"success","data":{"resultType":"matrix","result":[]}}
                """
        );

        long startTime = System.currentTimeMillis() - 10000;
        long endTime = System.currentTimeMillis();

        PrometheusExportService.PrometheusExportResult result = service.exportMetrics(startTime, endTime, "test");

        assertTrue(result.isSuccess());
    }

    @Test
    void exportMetricsShouldHandleMalformedJson() {
        setupRestClientMock(
                """
                {"status":"success","data":["metric1"]}
                """,
                "not valid json"
        );

        long startTime = System.currentTimeMillis() - 10000;
        long endTime = System.currentTimeMillis();

        PrometheusExportService.PrometheusExportResult result = service.exportMetrics(startTime, endTime, "test");

        assertTrue(result.isSuccess());
    }

    @Test
    void prometheusExportResultIsSuccessShouldReturnFalseWhenError() {
        var result = new PrometheusExportService.PrometheusExportResult(null, null, "Some error");

        assertFalse(result.isSuccess());
    }

    @Test
    void prometheusExportResultIsSuccessShouldReturnFalseWhenNoFilePath() {
        var result = new PrometheusExportService.PrometheusExportResult(null, "http://query", null);

        assertFalse(result.isSuccess());
    }

    @Test
    void prometheusExportResultIsSuccessShouldReturnTrueWhenValid() {
        var result = new PrometheusExportService.PrometheusExportResult("/path/file.json", "http://query", null);

        assertTrue(result.isSuccess());
    }

    @Test
    void prometheusExportResultRecordShouldStoreValues() {
        var result = new PrometheusExportService.PrometheusExportResult(
                "/path/file.json",
                "http://query",
                null
        );

        assertEquals("/path/file.json", result.filePath());
        assertEquals("http://query", result.queryUrl());
        assertNull(result.error());
    }

    @Test
    void exportMetricsShouldHandleNonArrayDataResult() {
        setupRestClientMock(
                """
                {"status":"success","data":["metric1"]}
                """,
                """
                {"status":"success","data":{"resultType":"matrix","result":"not an array"}}
                """
        );

        long startTime = System.currentTimeMillis() - 10000;
        long endTime = System.currentTimeMillis();

        PrometheusExportService.PrometheusExportResult result = service.exportMetrics(startTime, endTime, "test");

        assertTrue(result.isSuccess());
    }

    @Test
    void exportMetricsShouldHandleMissingStatusField() {
        setupRestClientMock(
                """
                {"data":["metric1"]}
                """,
                null
        );

        long startTime = System.currentTimeMillis() - 10000;
        long endTime = System.currentTimeMillis();

        PrometheusExportService.PrometheusExportResult result = service.exportMetrics(startTime, endTime, "test");

        assertTrue(result.isSuccess());
    }

    @Test
    void exportMetricsShouldHandleMissingDataField() {
        setupRestClientMock(
                """
                {"status":"success"}
                """,
                null
        );

        long startTime = System.currentTimeMillis() - 10000;
        long endTime = System.currentTimeMillis();

        PrometheusExportService.PrometheusExportResult result = service.exportMetrics(startTime, endTime, "test");

        assertTrue(result.isSuccess());
    }

    @Test
    void exportMetricsShouldHandleMetricQueryException() {
        var requestHeadersUriSpec = mock(RestClient.RequestHeadersUriSpec.class);
        var responseSpec = mock(RestClient.ResponseSpec.class);

        lenient().when(restClient.get()).thenReturn(requestHeadersUriSpec);
        lenient().when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersUriSpec);
        lenient().when(requestHeadersUriSpec.retrieve()).thenReturn(responseSpec);
        lenient().when(responseSpec.body(String.class))
                .thenReturn("""
                    {"status":"success","data":["metric1"]}
                    """)
                .thenThrow(new RuntimeException("Query failed"));

        long startTime = System.currentTimeMillis() - 10000;
        long endTime = System.currentTimeMillis();

        PrometheusExportService.PrometheusExportResult result = service.exportMetrics(startTime, endTime, "test");

        assertTrue(result.isSuccess());
    }

    @Test
    void exportMetricsShouldHandleMetricDataWithNullResultField() {
        setupRestClientMock(
                """
                {"status":"success","data":["metric1"]}
                """,
                """
                {"status":"success","data":{"resultType":"matrix"}}
                """
        );

        long startTime = System.currentTimeMillis() - 10000;
        long endTime = System.currentTimeMillis();

        PrometheusExportService.PrometheusExportResult result = service.exportMetrics(startTime, endTime, "test");

        assertTrue(result.isSuccess());
    }

    @Test
    void exportMetricsShouldHandleDirectoryCreationFailure() throws Exception {
        // Create a file where the directory should be created
        Path blockingFile = tempDir.resolve("blocking-file");
        Files.write(blockingFile, "blocking content".getBytes());

        // Try to create subdirectory inside a file (should fail)
        PrometheusExportService invalidPathService = new PrometheusExportService(
                new PrometheusProperties("http://localhost:9090", blockingFile.resolve("subdir").toString()),
                new PerfProperties(16000, 60000, 60000, 30000, 60, 15)
        );

        long startTime = System.currentTimeMillis() - 10000;
        long endTime = System.currentTimeMillis();

        PrometheusExportService.PrometheusExportResult result = invalidPathService.exportMetrics(startTime, endTime, "test");

        assertFalse(result.isSuccess());
        assertNotNull(result.error());
        assertTrue(result.error().contains("Failed to create export directory"));
    }

    @Test
    void exportMetricsShouldHandleFileWriteFailure() throws Exception {
        // Create a directory that is read-only (file write will fail)
        Path readOnlyDir = tempDir.resolve("readonly-dir");
        Files.createDirectories(readOnlyDir);

        // Create the expected filename as a directory to prevent file write
        String timestamp = java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")
                .format(java.time.Instant.now().atZone(java.time.ZoneId.systemDefault()));
        Path blockingDir = readOnlyDir.resolve("prometheus_export_" + timestamp + ".json");
        Files.createDirectories(blockingDir);  // Make it a directory so file write fails

        PrometheusExportService fileWriteFailService = new PrometheusExportService(
                new PrometheusProperties("http://localhost:9090", readOnlyDir.toString()),
                new PerfProperties(16000, 60000, 60000, 30000, 60, 15)
        );
        ReflectionTestUtils.setField(fileWriteFailService, "restClient", restClient);

        setupRestClientMock(
                """
                {"status":"success","data":[]}
                """,
                null
        );

        long startTime = System.currentTimeMillis() - 10000;
        long endTime = System.currentTimeMillis();

        PrometheusExportService.PrometheusExportResult result = fileWriteFailService.exportMetrics(startTime, endTime, "test");

        assertFalse(result.isSuccess());
        assertNotNull(result.error());
        assertTrue(result.error().contains("Failed to write export file"));
    }

    private void setupRestClientMock(String metricNamesResponse, String metricDataResponse) {
        var requestHeadersUriSpec = mock(RestClient.RequestHeadersUriSpec.class);
        var responseSpec = mock(RestClient.ResponseSpec.class);

        lenient().when(restClient.get()).thenReturn(requestHeadersUriSpec);
        lenient().when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersUriSpec);
        lenient().when(requestHeadersUriSpec.retrieve()).thenReturn(responseSpec);

        if (metricDataResponse != null) {
            lenient().when(responseSpec.body(String.class))
                    .thenReturn(metricNamesResponse)
                    .thenReturn(metricDataResponse)
                    .thenReturn(metricDataResponse);
        } else {
            lenient().when(responseSpec.body(String.class)).thenReturn(metricNamesResponse);
        }
    }
}
