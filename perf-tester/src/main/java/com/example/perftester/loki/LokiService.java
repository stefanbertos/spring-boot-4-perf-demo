package com.example.perftester.loki;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Slf4j
@Service
public class LokiService {

    private static final String DEFAULT_LEVEL = "INFO";
    private static final int DEFAULT_LIMIT = 5000;
    private static final String FALLBACK_QUERY = "{job=\"perf-tester\"}";
    private static final Pattern SPRING_LOG_PATTERN =
            Pattern.compile("^\\S+\\s+(TRACE|DEBUG|INFO|WARN|ERROR)\\s+");

    private final LokiProperties lokiProperties;
    private final LokiServiceLabelRepository labelRepository;
    private final RestClient restClient;

    public LokiService(LokiProperties lokiProperties,
                       LokiServiceLabelRepository labelRepository,
                       RestClient.Builder restClientBuilder) {
        this.lokiProperties = lokiProperties;
        this.labelRepository = labelRepository;
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
            var query = buildQuery();
            log.debug("Querying Loki with: {}", query);
            var response = restClient.get()
                    .uri(url + "/loki/api/v1/query_range?query={q}&start={s}&end={e}&limit={l}&direction=forward",
                            Map.of("q", query, "s", startNs, "e", endNs,
                                    "l", String.valueOf(DEFAULT_LIMIT)))
                    .retrieve()
                    .body(LokiResponse.class);
            return parseEntries(response);
        } catch (Exception e) {
            log.warn("Failed to query Loki logs: {}", e.getMessage());
            return List.of();
        }
    }

    private String buildQuery() {
        var names = labelRepository.findAll().stream()
                .map(LokiServiceLabel::getName)
                .toList();
        if (names.isEmpty()) {
            return FALLBACK_QUERY;
        }
        if (names.size() == 1) {
            return "{job=\"" + names.get(0) + "\"}";
        }
        var joined = String.join("|", names);
        return "{job=~\"" + joined + "\"}";
    }

    private List<LogEntry> parseEntries(LokiResponse response) {
        if (response == null || response.data() == null || response.data().result() == null) {
            return List.of();
        }
        var entries = new ArrayList<LogEntry>();
        for (var stream : response.data().result()) {
            if (stream.values() != null) {
                for (var value : stream.values()) {
                    if (value.size() >= 2) {
                        var tsNs = value.get(0);
                        var line = value.get(1);
                        var tsMillis = Long.parseLong(tsNs) / 1_000_000L;
                        var timestamp = Instant.ofEpochMilli(tsMillis).toString();
                        entries.add(new LogEntry(timestamp, extractLevel(line), extractMessage(line)));
                    }
                }
            }
        }
        entries.sort(Comparator.comparing(LogEntry::timestamp));
        return entries;
    }

    private static String extractLevel(String line) {
        var matcher = SPRING_LOG_PATTERN.matcher(line);
        return matcher.find() ? matcher.group(1) : DEFAULT_LEVEL;
    }

    private static String extractMessage(String line) {
        var matcher = SPRING_LOG_PATTERN.matcher(line);
        return matcher.find() ? line.substring(matcher.end()) : line;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record LokiResponse(LokiData data) { }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record LokiData(List<LokiStream> result) { }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record LokiStream(Map<String, String> stream, List<List<String>> values) { }
}
