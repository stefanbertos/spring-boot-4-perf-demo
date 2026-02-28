package com.example.perftester.rest;

import com.example.perftester.loki.LogEntry;
import com.example.perftester.loki.LokiService;
import com.example.perftester.persistence.TestRun;
import com.example.perftester.persistence.TestRunService;
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
import org.springframework.web.bind.annotation.RequestMapping;
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

    private final TestRunService testRunService;
    private final LokiService lokiService;

    public record TestRunListResponse(
            Long id,
            String testRunId,
            String testId,
            String status,
            int messageCount,
            long completedCount,
            Double tps,
            Double avgLatencyMs,
            Double minLatencyMs,
            Double maxLatencyMs,
            Double p25LatencyMs,
            Double p50LatencyMs,
            Double p75LatencyMs,
            Double p90LatencyMs,
            Double p95LatencyMs,
            Double p99LatencyMs,
            Long timeoutCount,
            String testType,
            String thresholdStatus,
            Long durationMs,
            Instant startedAt,
            Instant completedAt,
            String zipFilePath) {
    }

    public record TestRunDetailResponse(
            Long id,
            String testRunId,
            String testId,
            String status,
            int messageCount,
            long completedCount,
            Double tps,
            Double avgLatencyMs,
            Double minLatencyMs,
            Double maxLatencyMs,
            Double p25LatencyMs,
            Double p50LatencyMs,
            Double p75LatencyMs,
            Double p90LatencyMs,
            Double p95LatencyMs,
            Double p99LatencyMs,
            Long timeoutCount,
            String testType,
            String thresholdStatus,
            String thresholdResults,
            Long durationMs,
            Instant startedAt,
            Instant completedAt,
            String zipFilePath) {
    }

    public record TestRunSnapshotResponse(
            Long id,
            Long testRunId,
            Instant sampledAt,
            Integer outboundQueueDepth,
            Integer inboundQueueDepth,
            Long kafkaRequestsLag,
            Long kafkaResponsesLag) {
    }

    @Operation(summary = "List all test runs")
    @GetMapping
    public List<TestRunListResponse> listAll() {
        return testRunService.findAll().stream()
                .map(this::toListResponse)
                .toList();
    }

    @Operation(summary = "Get a test run by ID")
    @GetMapping("/{id}")
    public TestRunDetailResponse getById(@PathVariable long id) {
        var run = testRunService.findById(id);
        return toDetailResponse(run);
    }

    @Operation(summary = "Delete a test run")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable long id) {
        testRunService.delete(id);
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
                run.getDurationMs(), run.getStartedAt(), run.getCompletedAt(), run.getZipFilePath());
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
                run.getDurationMs(), run.getStartedAt(), run.getCompletedAt(), run.getZipFilePath());
    }
}
