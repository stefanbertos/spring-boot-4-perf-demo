package com.example.perftester.grafana;

import com.example.perftester.config.PerfProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import org.springframework.core.ParameterizedTypeReference;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Slf4j
@Service
public class GrafanaExportService {

    private final long bufferBeforeMs;
    private final long bufferAfterMs;
    private final RestClient restClient;
    private final String grafanaUrl;
    private final String exportPath;

    public GrafanaExportService(
            @Value("${app.grafana.url:http://localhost:3000}") String grafanaUrl,
            @Value("${app.grafana.export-path:./grafana-exports}") String exportPath,
            @Value("${app.grafana.api-key:}") String apiKey,
            PerfProperties perfProperties) {
        this.grafanaUrl = grafanaUrl;
        this.exportPath = exportPath;
        this.bufferBeforeMs = perfProperties.grafanaBufferBeforeMs();
        this.bufferAfterMs = perfProperties.grafanaBufferAfterMs();

        var builder = RestClient.builder()
                .baseUrl(grafanaUrl);

        if (apiKey != null && !apiKey.isEmpty()) {
            builder.defaultHeader("Authorization", "Bearer " + apiKey);
        } else {
            builder.defaultHeader("Authorization", "Basic " +
                    java.util.Base64.getEncoder().encodeToString("admin:admin".getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        }

        this.restClient = builder.build();
    }

    /**
     * Exports all Grafana dashboards as PNG images for the given test time window.
     *
     * <p>Adds a configurable buffer before and after the test window to capture
     * surrounding context. Each dashboard is rendered via the Grafana render API
     * and streamed directly to disk.
     *
     * @param testStartTimeMs epoch millis of test start
     * @param testEndTimeMs   epoch millis of test end
     * @return dashboard URLs and paths to exported image files
     */
    public DashboardExportResult exportDashboards(long testStartTimeMs, long testEndTimeMs) {
        var exported = new ArrayList<ExportedDashboard>();
        var timestamp = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")
                .format(Instant.now().atZone(java.time.ZoneId.systemDefault()));

        var exportDir = Path.of(exportPath);
        try {
            Files.createDirectories(exportDir);
        } catch (IOException e) {
            log.error("Failed to create export directory: {}", exportPath, e);
            return new DashboardExportResult(List.of(), List.of());
        }

        long fromMs = testStartTimeMs - bufferBeforeMs;
        long toMs = testEndTimeMs + bufferAfterMs;

        var dashboards = fetchAllDashboards();
        for (var dashboard : dashboards) {
            try {
                var result = exportDashboard(dashboard, fromMs, toMs, timestamp, exportDir);
                exported.add(result);
                log.info("Exported dashboard '{}' to: {} url:{}", dashboard.title(), result.filePath(), result.url());
            } catch (Exception e) {
                log.error("Failed to export dashboard '{}': {}", dashboard.title(), e.getMessage());
                // Add entry with URL only (no file)
                var url = buildDashboardUrl(dashboard.uid(), fromMs, toMs);
                exported.add(new ExportedDashboard(dashboard.uid(), dashboard.title(), url, null));
            }
        }

        var urls = exported.stream().map(ExportedDashboard::url).toList();
        var files = exported.stream()
                .map(ExportedDashboard::filePath)
                .filter(Objects::nonNull)
                .toList();

        return new DashboardExportResult(urls, files);
    }

    private List<DashboardInfo> fetchAllDashboards() {
        try {
            var response = restClient.get()
                    .uri("api/search?type=dash-db")
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<Map<String, Object>>>() {});

            if (response == null) {
                return List.of();
            }

            var dashboards = response.stream()
                    .filter(d -> d.get("uid") != null && d.get("title") != null)
                    .map(d -> new DashboardInfo((String) d.get("uid"), (String) d.get("title")))
                    .toList();

            log.info("Found {} dashboards in Grafana", dashboards.size());
            return dashboards;
        } catch (Exception e) {
            log.error("Failed to fetch dashboard list from Grafana: {}", e.getMessage());
            return List.of();
        }
    }

    private ExportedDashboard exportDashboard(DashboardInfo dashboard, long fromMs, long toMs,
                                               String timestamp, Path exportDir) throws IOException {
        // Use relative path (no leading slash) so it appends to baseUrl path
        var renderPath = String.format("render/d/%s/%s?from=%d&to=%d&width=2500&height=2500&tz=UTC",
                dashboard.uid(), dashboard.uid(), fromMs, toMs);

        var fullRenderUrl = grafanaUrl + "/" + renderPath;
        log.info("Fetching dashboard image from: {}", fullRenderUrl);

        var filename = String.format("%s_%s.png", dashboard.uid(), timestamp);
        var filePath = exportDir.resolve(filename);

        // Stream directly to file to avoid loading large images into memory
        var resource = restClient.get()
                .uri(renderPath)
                .retrieve()
                .body(Resource.class);

        if (resource == null) {
            throw new IOException("Empty response from Grafana render API");
        }

        try (InputStream inputStream = resource.getInputStream();
             OutputStream outputStream = Files.newOutputStream(filePath)) {
            inputStream.transferTo(outputStream);
        }

        if (Files.size(filePath) == 0) {
            Files.deleteIfExists(filePath);
            throw new IOException("Empty response from Grafana render API");
        }

        var dashboardUrl = buildDashboardUrl(dashboard.uid(), fromMs, toMs);

        return new ExportedDashboard(dashboard.uid(), dashboard.title(), dashboardUrl, filePath.toAbsolutePath().toString());
    }

    private String buildDashboardUrl(String uid, long fromMs, long toMs) {
        return String.format("%s/d/%s?from=%d&to=%d", grafanaUrl, uid, fromMs, toMs);
    }

    private record DashboardInfo(String uid, String title) {}

    public record ExportedDashboard(String uid, String title, String url, String filePath) {}

    public record DashboardExportResult(List<String> dashboardUrls, List<String> exportedFiles) {}
}
