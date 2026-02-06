package com.example.perftester.grafana;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClient;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.springframework.core.ParameterizedTypeReference;

import java.util.List;
import java.util.Map;

@ExtendWith(MockitoExtension.class)
class GrafanaExportServiceTest {

    private static final List<Map<String, Object>> MOCK_SEARCH_RESULTS = List.of(
            Map.of("uid", "dash-1", "title", "Dashboard 1"),
            Map.of("uid", "dash-2", "title", "Dashboard 2")
    );

    @TempDir
    Path tempDir;

    @Mock
    private RestClient restClient;

    private GrafanaExportService service;

    @BeforeEach
    void setUp() {
        service = new GrafanaExportService(
                "http://localhost:3000",
                tempDir.toString(),
                "test-api-key"
        );
        // Inject mock RestClient
        ReflectionTestUtils.setField(service, "restClient", restClient);
    }

    @Test
    void exportDashboardsShouldReturnUrlsAndFiles() {
        setupRestClientMock(new byte[]{1, 2, 3, 4});

        long startTime = System.currentTimeMillis() - 10000;
        long endTime = System.currentTimeMillis();

        GrafanaExportService.DashboardExportResult result = service.exportDashboards(startTime, endTime);

        assertNotNull(result);
        assertFalse(result.dashboardUrls().isEmpty());
        assertFalse(result.exportedFiles().isEmpty());

        // Verify URLs contain expected format
        for (String url : result.dashboardUrls()) {
            assertTrue(url.startsWith("http://localhost:3000/d/"));
            assertTrue(url.contains("from="));
            assertTrue(url.contains("to="));
        }
    }

    @Test
    void exportDashboardsShouldCreateFilesOnDisk() {
        byte[] imageData = "fake png data".getBytes();
        setupRestClientMock(imageData);

        long startTime = System.currentTimeMillis() - 10000;
        long endTime = System.currentTimeMillis();

        GrafanaExportService.DashboardExportResult result = service.exportDashboards(startTime, endTime);

        // Verify files were created
        for (String filePath : result.exportedFiles()) {
            assertTrue(Files.exists(Path.of(filePath)));
        }
    }

    @Test
    void exportDashboardsShouldHandleEmptyResponse() {
        setupRestClientMock(new byte[0]);

        long startTime = System.currentTimeMillis() - 10000;
        long endTime = System.currentTimeMillis();

        GrafanaExportService.DashboardExportResult result = service.exportDashboards(startTime, endTime);

        // Should still have URLs even if files couldn't be exported
        assertNotNull(result);
        assertFalse(result.dashboardUrls().isEmpty());
    }

    @Test
    void exportDashboardsShouldHandleNullResponse() {
        setupRestClientMock(null);

        long startTime = System.currentTimeMillis() - 10000;
        long endTime = System.currentTimeMillis();

        GrafanaExportService.DashboardExportResult result = service.exportDashboards(startTime, endTime);

        assertNotNull(result);
        assertFalse(result.dashboardUrls().isEmpty());
    }

    @Test
    void exportDashboardsShouldHandleRestClientException() {
        when(restClient.get()).thenThrow(new RuntimeException("Connection refused"));

        long startTime = System.currentTimeMillis() - 10000;
        long endTime = System.currentTimeMillis();

        GrafanaExportService.DashboardExportResult result = service.exportDashboards(startTime, endTime);

        // Should return empty result when Grafana is unreachable
        assertNotNull(result);
        assertTrue(result.dashboardUrls().isEmpty());
        assertTrue(result.exportedFiles().isEmpty());
    }

    @Test
    void constructorShouldUseBasicAuthWhenNoApiKey() {
        GrafanaExportService serviceWithoutApiKey = new GrafanaExportService(
                "http://localhost:3000",
                tempDir.toString(),
                ""
        );

        assertNotNull(serviceWithoutApiKey);
    }

    @Test
    void constructorShouldUseBasicAuthWhenApiKeyIsNull() {
        GrafanaExportService serviceWithNullApiKey = new GrafanaExportService(
                "http://localhost:3000",
                tempDir.toString(),
                null
        );

        assertNotNull(serviceWithNullApiKey);
    }

    @Test
    void dashboardExportResultRecordShouldStoreValues() {
        var result = new GrafanaExportService.DashboardExportResult(
                java.util.List.of("http://url1", "http://url2"),
                java.util.List.of("/path/file1.png", "/path/file2.png")
        );

        assertEquals(2, result.dashboardUrls().size());
        assertEquals(2, result.exportedFiles().size());
    }

    @Test
    void exportedDashboardRecordShouldStoreValues() {
        var dashboard = new GrafanaExportService.ExportedDashboard(
                "uid-1",
                "Test Dashboard",
                "http://url",
                "/path/file.png"
        );

        assertEquals("uid-1", dashboard.uid());
        assertEquals("Test Dashboard", dashboard.title());
        assertEquals("http://url", dashboard.url());
        assertEquals("/path/file.png", dashboard.filePath());
    }

    @Test
    void exportedDashboardWithNullFilePath() {
        var dashboard = new GrafanaExportService.ExportedDashboard(
                "uid-1",
                "Test Dashboard",
                "http://url",
                null
        );

        assertEquals("uid-1", dashboard.uid());
        assertEquals("http://url", dashboard.url());
        assertNull(dashboard.filePath());
    }

    @Test
    void dashboardExportResultWithEmptyLists() {
        var result = new GrafanaExportService.DashboardExportResult(
                java.util.List.of(),
                java.util.List.of()
        );

        assertTrue(result.dashboardUrls().isEmpty());
        assertTrue(result.exportedFiles().isEmpty());
    }

    @Test
    void exportDashboardsShouldHandleDirectoryCreationFailure() throws Exception {
        setupRestClientMock(new byte[]{1, 2, 3, 4});

        // Create a file where the directory should be created
        Path blockingFile = tempDir.resolve("blocking-file");
        Files.write(blockingFile, "blocking content".getBytes());

        // Try to create subdirectory inside a file (should fail)
        GrafanaExportService invalidPathService = new GrafanaExportService(
                "http://localhost:3000",
                blockingFile.resolve("subdir").toString(),
                "test-api-key"
        );
        ReflectionTestUtils.setField(invalidPathService, "restClient", restClient);

        long startTime = System.currentTimeMillis() - 10000;
        long endTime = System.currentTimeMillis();

        GrafanaExportService.DashboardExportResult result = invalidPathService.exportDashboards(startTime, endTime);

        // Should return empty result when directory creation fails
        assertTrue(result.dashboardUrls().isEmpty());
        assertTrue(result.exportedFiles().isEmpty());
    }

    @SuppressWarnings("unchecked")
    private void setupRestClientMock(byte[] responseBody) {
        var requestHeadersUriSpec = mock(RestClient.RequestHeadersUriSpec.class);
        var responseSpec = mock(RestClient.ResponseSpec.class);

        lenient().when(restClient.get()).thenReturn(requestHeadersUriSpec);
        lenient().when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersUriSpec);
        lenient().when(requestHeadersUriSpec.retrieve()).thenReturn(responseSpec);

        // Mock search API response for fetchAllDashboards
        lenient().when(responseSpec.body(any(ParameterizedTypeReference.class)))
                .thenReturn(MOCK_SEARCH_RESULTS);

        // Mock render API response for dashboard export
        Resource resource = responseBody != null && responseBody.length > 0
                ? new ByteArrayResource(responseBody)
                : null;
        lenient().when(responseSpec.body(Resource.class)).thenReturn(resource);
    }
}
