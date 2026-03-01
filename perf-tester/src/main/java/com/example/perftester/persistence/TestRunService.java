package com.example.perftester.persistence;

import com.example.perftester.perf.PerfTestResult;
import com.example.perftester.perf.ThresholdResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.TreeSet;

@Slf4j
@Service
@RequiredArgsConstructor
public class TestRunService {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final TestRunRepository testRunRepository;
    private final TestRunSnapshotRepository snapshotRepository;

    @Transactional
    public TestRun createRun(String testRunId, String testId, int messageCount, String testType) {
        var run = new TestRun();
        run.setTestRunId(testRunId);
        run.setTestId(testId);
        run.setStatus("RUNNING");
        run.setMessageCount(messageCount);
        run.setCompletedCount(0L);
        run.setTestType(testType);
        run.setStartedAt(Instant.now());
        return testRunRepository.save(run);
    }

    @Transactional
    public void completeRun(Long id, String status, PerfTestResult result, String zipFilePath) {
        var run = testRunRepository.findById(id)
                .orElseThrow(() -> new TestRunNotFoundException(id));
        run.setStatus(status);
        run.setCompletedCount(result.completedMessages());
        run.setTps(result.tps());
        run.setAvgLatencyMs(result.avgLatencyMs());
        run.setMinLatencyMs(result.minLatencyMs());
        run.setMaxLatencyMs(result.maxLatencyMs());
        run.setP25LatencyMs(result.p25LatencyMs());
        run.setP50LatencyMs(result.p50LatencyMs());
        run.setP75LatencyMs(result.p75LatencyMs());
        run.setP90LatencyMs(result.p90LatencyMs());
        run.setP95LatencyMs(result.p95LatencyMs());
        run.setP99LatencyMs(result.p99LatencyMs());
        run.setTimeoutCount(result.pendingMessages());
        run.setDurationMs(Math.round(result.testDurationSeconds() * 1000));
        run.setZipFilePath(zipFilePath);
        run.setCompletedAt(Instant.now());
        testRunRepository.save(run);
    }

    @Transactional
    public void updateThresholdResult(Long id, String status, List<ThresholdResult> results) {
        var run = testRunRepository.findById(id)
                .orElseThrow(() -> new TestRunNotFoundException(id));
        run.setThresholdStatus(status);
        try {
            run.setThresholdResults(MAPPER.writeValueAsString(results));
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize threshold results for run {}: {}", id, e.getMessage());
            run.setThresholdResults("[]");
        }
        testRunRepository.save(run);
    }

    @Transactional(readOnly = true)
    public List<TestRun> findAll() {
        return testRunRepository.findAllByOrderByStartedAtDesc();
    }

    @Transactional(readOnly = true)
    public Page<TestRun> findAll(int page, int size, String tag) {
        var pageable = PageRequest.of(page, size);
        if (tag != null && !tag.isBlank()) {
            return testRunRepository.findByTagsLike("%" + tag + "%", pageable);
        }
        return testRunRepository.findAllByOrderByStartedAtDesc(pageable);
    }

    @Transactional(readOnly = true)
    public TestRun findById(Long id) {
        return testRunRepository.findById(id)
                .orElseThrow(() -> new TestRunNotFoundException(id));
    }

    @Transactional(readOnly = true)
    public List<TestRunSnapshot> findSnapshots(Long id) {
        testRunRepository.findById(id).orElseThrow(() -> new TestRunNotFoundException(id));
        return snapshotRepository.findByTestRunIdOrderBySampledAtAsc(id);
    }

    @Transactional
    public void setTags(Long id, List<String> tags) {
        var run = testRunRepository.findById(id)
                .orElseThrow(() -> new TestRunNotFoundException(id));
        try {
            run.setTags(MAPPER.writeValueAsString(tags));
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize tags for run {}: {}", id, e.getMessage());
            run.setTags("[]");
        }
        testRunRepository.save(run);
    }

    @Transactional(readOnly = true)
    public List<String> getAllUniqueTags() {
        var allJson = testRunRepository.findAllTagsJson();
        var result = new TreeSet<String>();
        for (var json : allJson) {
            try {
                List<String> parsed = MAPPER.readValue(json, new TypeReference<>() {});
                result.addAll(parsed);
            } catch (IOException e) {
                log.debug("Failed to parse tags JSON: {}", json);
            }
        }
        return List.copyOf(result);
    }

    @Transactional
    public void bulkDelete(List<Long> ids) {
        for (var id : ids) {
            delete(id);
        }
    }

    @Transactional(readOnly = true)
    public List<TestRun> getTrendData() {
        return testRunRepository.findCompletedOrderByStartedAtAsc();
    }

    @Transactional
    public void delete(Long id) {
        var run = testRunRepository.findById(id)
                .orElseThrow(() -> new TestRunNotFoundException(id));
        if (run.getZipFilePath() != null) {
            try {
                Files.deleteIfExists(Path.of(run.getZipFilePath()));
                log.info("Deleted ZIP file: {}", run.getZipFilePath());
            } catch (IOException e) {
                log.warn("Failed to delete ZIP file {}: {}", run.getZipFilePath(), e.getMessage());
            }
        }
        testRunRepository.deleteById(id);
    }
}
