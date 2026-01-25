package com.example.perftester.prometheus;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
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

@Slf4j
@Service
public class PrometheusExportService {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String exportPath;

    public PrometheusExportService(
            @Value("${app.prometheus.url:http://localhost:9090}") String prometheusUrl,
            @Value("${app.prometheus.export-path:./prometheus-exports}") String exportPath) {
        this.restClient = RestClient.builder()
                .baseUrl(prometheusUrl)
                .build();
        this.objectMapper = new ObjectMapper()
                .enable(SerializationFeature.INDENT_OUTPUT);
        this.exportPath = exportPath;
    }

    public PrometheusExportResult exportMetrics(long testStartTimeMs, long testEndTimeMs, String testId) {
        String timestamp = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")
                .format(Instant.now().atZone(java.time.ZoneId.systemDefault()));

        Path exportDir = Path.of(exportPath);
        try {
            Files.createDirectories(exportDir);
        } catch (IOException e) {
            log.error("Failed to create export directory: {}", exportPath, e);
            return new PrometheusExportResult(null, null, "Failed to create export directory: " + e.getMessage());
        }

        // Add buffer for metric collection delay
        long fromSec = (testStartTimeMs / 1000) - 60;
        long toSec = (testEndTimeMs / 1000) + 60;
        int step = 15; // 15 second resolution

        ObjectNode exportData = objectMapper.createObjectNode();
        exportData.put("testId", testId != null ? testId : timestamp);
        exportData.put("exportTimestamp", Instant.now().toString());
        exportData.put("testStartTime", Instant.ofEpochMilli(testStartTimeMs).toString());
        exportData.put("testEndTime", Instant.ofEpochMilli(testEndTimeMs).toString());
        exportData.put("fromEpochSeconds", fromSec);
        exportData.put("toEpochSeconds", toSec);
        exportData.put("stepSeconds", step);

        ArrayNode metricsArray = exportData.putArray("metrics");

        List<String> allMetrics = getAllMetricNames();
        log.info("Found {} metrics to export", allMetrics.size());

        int exported = 0;
        for (String metric : allMetrics) {
            try {
                JsonNode metricData = queryMetricRange(metric, fromSec, toSec, step);
                if (metricData != null && metricData.has("data") && metricData.get("data").has("result")) {
                    JsonNode result = metricData.get("data").get("result");
                    if (result.isArray() && !result.isEmpty()) {
                        ObjectNode metricNode = objectMapper.createObjectNode();
                        metricNode.put("name", metric);
                        metricNode.set("data", result);
                        metricsArray.add(metricNode);
                        exported++;
                        log.debug("Exported metric: {}", metric);
                    }
                }
            } catch (Exception e) {
                log.warn("Failed to export metric {}: {}", metric, e.getMessage());
            }
        }
        log.info("Exported {} metrics with data", exported);

        String filename = String.format("prometheus_export_%s.json", timestamp);
        Path filePath = exportDir.resolve(filename);

        try {
            objectMapper.writeValue(filePath.toFile(), exportData);
            log.info("Prometheus metrics exported to: {}", filePath.toAbsolutePath());

            String queryUrl = buildQueryRangeUrl(fromSec, toSec);
            log.info("Prometheus query URL pattern: {}", queryUrl);

            return new PrometheusExportResult(
                    filePath.toAbsolutePath().toString(),
                    queryUrl,
                    null
            );
        } catch (IOException e) {
            log.error("Failed to write export file: {}", e.getMessage());
            return new PrometheusExportResult(null, null, "Failed to write export file: " + e.getMessage());
        }
    }

    private List<String> getAllMetricNames() {
        try {
            String response = restClient.get()
                    .uri("/api/v1/label/__name__/values")
                    .retrieve()
                    .body(String.class);

            JsonNode root = objectMapper.readTree(response);
            if (root.has("status") && "success".equals(root.get("status").asText()) && root.has("data")) {
                List<String> metrics = new ArrayList<>();
                for (JsonNode metricName : root.get("data")) {
                    metrics.add(metricName.asText());
                }
                return metrics;
            }
        } catch (Exception e) {
            log.error("Failed to fetch metric names from Prometheus: {}", e.getMessage());
        }
        return List.of();
    }

    private JsonNode queryMetricRange(String metric, long fromSec, long toSec, int step) {
        String uri = String.format("/api/v1/query_range?query=%s&start=%d&end=%d&step=%d",
                metric, fromSec, toSec, step);

        String response = restClient.get()
                .uri(uri)
                .retrieve()
                .body(String.class);

        try {
            return objectMapper.readTree(response);
        } catch (Exception e) {
            log.warn("Failed to parse response for {}: {}", metric, e.getMessage());
            return null;
        }
    }

    private String buildQueryRangeUrl(long fromSec, long toSec) {
        return String.format("/api/v1/query_range?query={metric}&start=%d&end=%d&step=15", fromSec, toSec);
    }

    public record PrometheusExportResult(String filePath, String queryUrl, String error) {
        public boolean isSuccess() {
            return error == null && filePath != null;
        }
    }
}
