package com.example.perftester.prometheus;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
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
        var timestamp = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")
                .format(Instant.now().atZone(java.time.ZoneId.systemDefault()));

        var exportDir = Path.of(exportPath);
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

        var filename = String.format("prometheus_export_%s.json", timestamp);
        var filePath = exportDir.resolve(filename);

        var allMetrics = getAllMetricNames();
        log.info("Found {} metrics to export", allMetrics.size());

        // Stream metrics directly to file to avoid OOM with large metric sets
        int exported = 0;
        try (OutputStream os = new BufferedOutputStream(Files.newOutputStream(filePath));
             JsonGenerator generator = objectMapper.getFactory().createGenerator(os)) {

            generator.useDefaultPrettyPrinter();
            generator.writeStartObject();

            // Write header fields
            generator.writeStringField("testId", testId != null ? testId : timestamp);
            generator.writeStringField("exportTimestamp", Instant.now().toString());
            generator.writeStringField("testStartTime", Instant.ofEpochMilli(testStartTimeMs).toString());
            generator.writeStringField("testEndTime", Instant.ofEpochMilli(testEndTimeMs).toString());
            generator.writeNumberField("fromEpochSeconds", fromSec);
            generator.writeNumberField("toEpochSeconds", toSec);
            generator.writeNumberField("stepSeconds", step);

            // Stream metrics array
            generator.writeArrayFieldStart("metrics");

            for (var metric : allMetrics) {
                try {
                    var metricData = queryMetricRange(metric, fromSec, toSec, step);
                    if (metricData != null && metricData.has("data") && metricData.get("data").has("result")) {
                        var result = metricData.get("data").get("result");
                        if (result.isArray() && !result.isEmpty()) {
                            generator.writeStartObject();
                            generator.writeStringField("name", metric);
                            generator.writeFieldName("data");
                            generator.writeTree(result);
                            generator.writeEndObject();
                            generator.flush(); // Flush after each metric to free memory
                            exported++;
                            log.debug("Exported metric: {}", metric);
                        }
                    }
                } catch (Exception e) {
                    log.warn("Failed to export metric {}: {}", metric, e.getMessage());
                }
            }

            generator.writeEndArray();
            generator.writeEndObject();

        } catch (IOException e) {
            log.error("Failed to write export file: {}", e.getMessage());
            return new PrometheusExportResult(null, null, "Failed to write export file: " + e.getMessage());
        }

        log.info("Exported {} metrics with data", exported);
        log.info("Prometheus metrics exported to: {}", filePath.toAbsolutePath());

        var queryUrl = buildQueryRangeUrl(fromSec, toSec);
        log.info("Prometheus query URL pattern: {}", queryUrl);

        return new PrometheusExportResult(
                filePath.toAbsolutePath().toString(),
                queryUrl,
                null
        );
    }

    private List<String> getAllMetricNames() {
        try {
            var response = restClient.get()
                    .uri("/api/v1/label/__name__/values")
                    .retrieve()
                    .body(String.class);

            var root = objectMapper.readTree(response);
            if (root.has("status") && "success".equals(root.get("status").asText()) && root.has("data")) {
                var metrics = new ArrayList<String>();
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
        var uri = String.format("/api/v1/query_range?query=%s&start=%d&end=%d&step=%d",
                metric, fromSec, toSec, step);

        var response = restClient.get()
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
