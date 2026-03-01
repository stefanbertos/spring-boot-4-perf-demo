package com.example.perftester.rest;

import com.example.perftester.loki.LogEntry;
import com.example.perftester.loki.LokiService;
import com.example.perftester.persistence.TestRun;
import com.example.perftester.persistence.TestRunService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

@Tag(name = "Test Runs", description = "Query and manage historical test run records and results")
@Slf4j
@RestController
@RequestMapping("/api/perf/test-runs")
@RequiredArgsConstructor
public class TestRunController {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final TestRunService testRunService;
    private final LokiService lokiService;

    @Operation(summary = "List test runs with pagination")
    @GetMapping
    public PagedResponse<TestRunListResponse> listAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String tag) {
        var result = testRunService.findAll(page, size, tag);
        var content = result.getContent().stream().map(this::toListResponse).toList();
        return new PagedResponse<>(content, result.getNumber(), result.getSize(),
                result.getTotalElements(), result.getTotalPages());
    }

    @Operation(summary = "Get all unique tags across test runs")
    @GetMapping("/tags")
    public List<String> getAllTags() {
        return testRunService.getAllUniqueTags();
    }

    @Operation(summary = "Get trend data for completed test runs")
    @GetMapping("/trends")
    public List<TrendPoint> getTrends() {
        return testRunService.getTrendData().stream().map(this::toTrendPoint).toList();
    }

    @Operation(summary = "Get a test run by ID")
    @GetMapping("/{id}")
    public TestRunDetailResponse getById(@PathVariable long id) {
        var run = testRunService.findById(id);
        return toDetailResponse(run);
    }

    @Operation(summary = "Set tags on a test run")
    @PutMapping("/{id}/tags")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void setTags(@PathVariable long id, @RequestBody SetTagsRequest request) {
        testRunService.setTags(id, request.tags());
    }

    @Operation(summary = "Delete a test run")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable long id) {
        testRunService.delete(id);
    }

    @Operation(summary = "Bulk delete test runs")
    @DeleteMapping("/bulk")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void bulkDelete(@RequestBody BulkDeleteRequest request) {
        testRunService.bulkDelete(request.ids());
    }

    @Operation(summary = "Download test results ZIP", description = "Downloads the packaged ZIP file containing dashboards, metrics, logs, and summary for this run")
    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> downloadZip(@PathVariable long id) {
        var run = testRunService.findById(id);
        if (run.getZipFilePath() == null) {
            return ResponseEntity.notFound().build();
        }
        var path = Path.of(run.getZipFilePath());
        var resource = new FileSystemResource(path);
        if (!resource.exists()) {
            return ResponseEntity.notFound().build();
        }
        var filename = path.getFileName().toString();
        var headers = new HttpHeaders();
        headers.setContentDisposition(ContentDisposition.attachment().filename(filename).build());
        return ResponseEntity.ok()
                .headers(headers)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }

    @Operation(summary = "Get application logs for a test run", description = "Queries Loki for application logs in the time window of the test run")
    @GetMapping("/{id}/logs")
    public List<LogEntry> getLogs(@PathVariable long id) {
        var run = testRunService.findById(id);
        var end = run.getCompletedAt() != null ? run.getCompletedAt() : Instant.now();
        return lokiService.queryLogs(run.getStartedAt(), end);
    }

    @Operation(summary = "Get infrastructure snapshots for a test run", description = "Returns queue depth and Kafka consumer lag snapshots captured during the test")
    @GetMapping("/{id}/snapshots")
    public List<TestRunSnapshotResponse> getSnapshots(@PathVariable long id) {
        return testRunService.findSnapshots(id).stream()
                .map(s -> new TestRunSnapshotResponse(s.getId(), s.getTestRunId(), s.getSampledAt(),
                        s.getOutboundQueueDepth(), s.getInboundQueueDepth(),
                        s.getKafkaRequestsLag(), s.getKafkaResponsesLag()))
                .toList();
    }

    private TestRunListResponse toListResponse(TestRun run) {
        return new TestRunListResponse(
                run.getId(), run.getTestRunId(), run.getTestId(), run.getStatus(),
                run.getMessageCount(), run.getCompletedCount(),
                run.getTps(), run.getAvgLatencyMs(), run.getMinLatencyMs(), run.getMaxLatencyMs(),
                run.getP25LatencyMs(), run.getP50LatencyMs(), run.getP75LatencyMs(),
                run.getP90LatencyMs(), run.getP95LatencyMs(), run.getP99LatencyMs(),
                run.getTimeoutCount(), run.getTestType(), run.getThresholdStatus(),
                run.getDurationMs(), run.getStartedAt(), run.getCompletedAt(), run.getZipFilePath(),
                parseTags(run.getTags()));
    }

    private TestRunDetailResponse toDetailResponse(TestRun run) {
        return new TestRunDetailResponse(
                run.getId(), run.getTestRunId(), run.getTestId(), run.getStatus(),
                run.getMessageCount(), run.getCompletedCount(),
                run.getTps(), run.getAvgLatencyMs(), run.getMinLatencyMs(), run.getMaxLatencyMs(),
                run.getP25LatencyMs(), run.getP50LatencyMs(), run.getP75LatencyMs(),
                run.getP90LatencyMs(), run.getP95LatencyMs(), run.getP99LatencyMs(),
                run.getTimeoutCount(), run.getTestType(), run.getThresholdStatus(),
                run.getThresholdResults(),
                run.getDurationMs(), run.getStartedAt(), run.getCompletedAt(), run.getZipFilePath(),
                parseTags(run.getTags()));
    }

    private TrendPoint toTrendPoint(TestRun run) {
        return new TrendPoint(run.getId(), run.getTestId(), run.getStartedAt(),
                run.getTps(), run.getP99LatencyMs(), run.getStatus(), run.getTestType());
    }

    private List<String> parseTags(String tagsJson) {
        if (tagsJson == null) {
            return List.of();
        }
        try {
            return MAPPER.readValue(tagsJson, new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            log.warn("Failed to parse tags JSON: {}", e.getMessage());
            return List.of();
        }
    }
}
