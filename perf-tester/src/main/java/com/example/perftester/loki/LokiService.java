package com.example.perftester.loki;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@EnableConfigurationProperties(LokiProperties.class)
public class LokiService {

    private static final String LOG_LEVEL_LABEL = "level";
    private static final String DEFAULT_LEVEL = "INFO";
    private static final int DEFAULT_LIMIT = 500;

    private final LokiProperties lokiProperties;
    private final RestClient restClient;

    public LokiService(LokiProperties lokiProperties, RestClient.Builder restClientBuilder) {
        this.lokiProperties = lokiProperties;
        this.restClient = restClientBuilder.build();
    }

    public List<LogEntry> queryLogs(Instant start, Instant end) {
        var url = lokiProperties.url();
        if (url == null || url.isBlank()) {
            log.debug("Loki URL not configured, returning empty log list");
            return List.of();
        }
        try {
            var startNs = String.valueOf(start.toEpochMilli() * 1_000_000L);
            var endNs = String.valueOf(end.toEpochMilli() * 1_000_000L);
            var response = restClient.get()
                    .uri(url + "/loki/api/v1/query_range?query={q}&start={s}&end={e}&limit={l}&direction=forward",
                            Map.of("q", "{job=\"perf-tester\"}", "s", startNs, "e", endNs,
                                    "l", String.valueOf(DEFAULT_LIMIT)))
                    .retrieve()
                    .body(LokiResponse.class);
            return parseEntries(response);
        } catch (Exception e) {
            log.warn("Failed to query Loki logs: {}", e.getMessage());
            return List.of();
        }
    }

    private List<LogEntry> parseEntries(LokiResponse response) {
        if (response == null || response.data() == null || response.data().result() == null) {
            return List.of();
        }
        var entries = new ArrayList<LogEntry>();
        for (var stream : response.data().result()) {
            var labels = stream.stream() != null ? stream.stream() : Map.<String, String>of();
            var level = labels.getOrDefault(LOG_LEVEL_LABEL, DEFAULT_LEVEL).toUpperCase();
            if (stream.values() != null) {
                for (var value : stream.values()) {
                    if (value.size() >= 2) {
                        var tsNs = value.get(0);
                        var line = value.get(1);
                        var tsMillis = Long.parseLong(tsNs) / 1_000_000L;
                        var timestamp = Instant.ofEpochMilli(tsMillis).toString();
                        entries.add(new LogEntry(timestamp, level, line));
                    }
                }
            }
        }
        return entries;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record LokiResponse(LokiData data) { }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record LokiData(List<LokiStream> result) { }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record LokiStream(Map<String, String> stream, List<List<String>> values) { }
}
