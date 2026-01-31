package com.example.perftester.grafana;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Slf4j
@Service
public class GrafanaExportService {

    private final RestClient restClient;
    private final String grafanaUrl;
    private final String exportPath;

    private static final List<DashboardInfo> DASHBOARDS = List.of(
            new DashboardInfo("perf-tester", "Perf-tester"),
            new DashboardInfo("ibm-mq", "IBM MQ"),
            new DashboardInfo("kafka", "Kafka"),
            new DashboardInfo("ibm-mq-consumer", "IBM Mq Consumer"),
            new DashboardInfo("kafka-consumer", "Kafka Consumer"),
            new DashboardInfo("k8s", "K8S Dashboard"),
            new DashboardInfo("node-exporter", "Node Exporter"),
            new DashboardInfo("tempo-tracing", "Tempo Tracing"),
            new DashboardInfo("kafka-exporter", "Kafka Exporter")
    );

    public GrafanaExportService(
            @Value("${app.grafana.url:http://localhost:3000}") String grafanaUrl,
            @Value("${app.grafana.export-path:./grafana-exports}") String exportPath,
            @Value("${app.grafana.api-key:}") String apiKey) {
        this.grafanaUrl = grafanaUrl;
        this.exportPath = exportPath;

        RestClient.Builder builder = RestClient.builder()
                .baseUrl(grafanaUrl);

        if (apiKey != null && !apiKey.isEmpty()) {
            builder.defaultHeader("Authorization", "Bearer " + apiKey);
        } else {
            builder.defaultHeader("Authorization", "Basic " +
                    java.util.Base64.getEncoder().encodeToString("admin:admin".getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        }

        this.restClient = builder.build();
    }

    public DashboardExportResult exportDashboards(long testStartTimeMs, long testEndTimeMs) {
        List<ExportedDashboard> exported = new ArrayList<>();
        String timestamp = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")
                .format(Instant.now().atZone(java.time.ZoneId.systemDefault()));

        Path exportDir = Path.of(exportPath);
        try {
            Files.createDirectories(exportDir);
        } catch (IOException e) {
            log.error("Failed to create export directory: {}", exportPath, e);
            return new DashboardExportResult(List.of(), List.of());
        }

        // Add buffer time for metrics to be visible
        long fromMs = testStartTimeMs - 60000; // 1 min before
        long toMs = testEndTimeMs + 30000;     // 30 sec after

        for (DashboardInfo dashboard : DASHBOARDS) {
            try {
                ExportedDashboard result = exportDashboard(dashboard, fromMs, toMs, timestamp, exportDir);
                exported.add(result);
                log.info("Exported dashboard '{}' to: {}", dashboard.title(), result.filePath());
                log.info("Dashboard URL: {}", result.url());
            } catch (Exception e) {
                log.error("Failed to export dashboard '{}': {}", dashboard.title(), e.getMessage());
                // Add entry with URL only (no file)
                String url = buildDashboardUrl(dashboard.uid(), fromMs, toMs);
                exported.add(new ExportedDashboard(dashboard.uid(), dashboard.title(), url, null));
            }
        }

        List<String> urls = exported.stream().map(ExportedDashboard::url).toList();
        List<String> files = exported.stream()
                .map(ExportedDashboard::filePath)
                .filter(Objects::nonNull)
                .toList();

        return new DashboardExportResult(urls, files);
    }

    private ExportedDashboard exportDashboard(DashboardInfo dashboard, long fromMs, long toMs,
                                               String timestamp, Path exportDir) throws IOException {
        // Use relative path (no leading slash) so it appends to baseUrl path
        String renderPath = String.format("render/d/%s/%s?from=%d&to=%d&width=2500&height=2500&tz=UTC",
                dashboard.uid(), dashboard.uid(), fromMs, toMs);

        String fullRenderUrl = grafanaUrl + "/" + renderPath;
        log.info("Fetching dashboard image from: {}", fullRenderUrl);

        byte[] imageBytes = restClient.get()
                .uri(renderPath)
                .retrieve()
                .body(byte[].class);

        if (imageBytes == null || imageBytes.length == 0) {
            throw new IOException("Empty response from Grafana render API");
        }

        String filename = String.format("%s_%s.png", dashboard.uid(), timestamp);
        Path filePath = exportDir.resolve(filename);
        Files.write(filePath, imageBytes);

        String dashboardUrl = buildDashboardUrl(dashboard.uid(), fromMs, toMs);

        return new ExportedDashboard(dashboard.uid(), dashboard.title(), dashboardUrl, filePath.toAbsolutePath().toString());
    }

    private String buildDashboardUrl(String uid, long fromMs, long toMs) {
        return String.format("%s/d/%s?from=%d&to=%d", grafanaUrl, uid, fromMs, toMs);
    }

    private record DashboardInfo(String uid, String title) {}

    public record ExportedDashboard(String uid, String title, String url, String filePath) {}

    public record DashboardExportResult(List<String> dashboardUrls, List<String> exportedFiles) {}
}
