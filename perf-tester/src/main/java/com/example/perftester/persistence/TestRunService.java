package com.example.perftester.persistence;

import com.example.perftester.perf.PerfTestResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TestRunService {

    private final TestRunRepository testRunRepository;

    @Transactional
    public TestRun createRun(String testRunId, String testId, int messageCount) {
        var run = new TestRun();
        run.setTestRunId(testRunId);
        run.setTestId(testId);
        run.setStatus("RUNNING");
        run.setMessageCount(messageCount);
        run.setCompletedCount(0L);
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
        run.setDurationMs(Math.round(result.testDurationSeconds() * 1000));
        run.setZipFilePath(zipFilePath);
        run.setCompletedAt(Instant.now());
        testRunRepository.save(run);
    }

    @Transactional(readOnly = true)
    public List<TestRun> findAll() {
        return testRunRepository.findAllByOrderByStartedAtDesc();
    }

    @Transactional(readOnly = true)
    public TestRun findById(Long id) {
        return testRunRepository.findById(id)
                .orElseThrow(() -> new TestRunNotFoundException(id));
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
