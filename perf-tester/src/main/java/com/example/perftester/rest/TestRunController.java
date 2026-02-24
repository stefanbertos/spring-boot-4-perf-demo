package com.example.perftester.rest;

import com.example.perftester.loki.LogEntry;
import com.example.perftester.loki.LokiService;
import com.example.perftester.persistence.TestRun;
import com.example.perftester.persistence.TestRunService;
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
            Double p50LatencyMs,
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
            Double p50LatencyMs,
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

    @GetMapping
    public List<TestRunListResponse> listAll() {
        return testRunService.findAll().stream()
                .map(this::toListResponse)
                .toList();
    }

    @GetMapping("/{id}")
    public TestRunDetailResponse getById(@PathVariable long id) {
        var run = testRunService.findById(id);
        return toDetailResponse(run);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable long id) {
        testRunService.delete(id);
    }

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

    @GetMapping("/{id}/logs")
    public List<LogEntry> getLogs(@PathVariable long id) {
        var run = testRunService.findById(id);
        var end = run.getCompletedAt() != null ? run.getCompletedAt() : Instant.now();
        return lokiService.queryLogs(run.getStartedAt(), end);
    }

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
                run.getP50LatencyMs(), run.getP90LatencyMs(), run.getP95LatencyMs(), run.getP99LatencyMs(),
                run.getTimeoutCount(), run.getTestType(), run.getThresholdStatus(),
                run.getDurationMs(), run.getStartedAt(), run.getCompletedAt(), run.getZipFilePath());
    }

    private TestRunDetailResponse toDetailResponse(TestRun run) {
        return new TestRunDetailResponse(
                run.getId(), run.getTestRunId(), run.getTestId(), run.getStatus(),
                run.getMessageCount(), run.getCompletedCount(),
                run.getTps(), run.getAvgLatencyMs(), run.getMinLatencyMs(), run.getMaxLatencyMs(),
                run.getP50LatencyMs(), run.getP90LatencyMs(), run.getP95LatencyMs(), run.getP99LatencyMs(),
                run.getTimeoutCount(), run.getTestType(), run.getThresholdStatus(),
                run.getThresholdResults(),
                run.getDurationMs(), run.getStartedAt(), run.getCompletedAt(), run.getZipFilePath());
    }
}
