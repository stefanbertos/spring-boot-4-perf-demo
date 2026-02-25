package com.example.perftester.monitoring;

import com.example.perftester.admin.IbmMqAdminService;
import com.example.perftester.admin.KafkaAdminService;
import com.example.perftester.config.MonitoringProperties;
import com.example.perftester.persistence.TestRunSnapshot;
import com.example.perftester.persistence.TestRunSnapshotRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class InfraSnapshotService {

    private static final int POLL_INTERVAL_SECONDS = 10;

    private final IbmMqAdminService ibmMqAdminService;
    private final KafkaAdminService kafkaAdminService;
    private final TestRunSnapshotRepository snapshotRepository;
    private final MonitoringProperties monitoringProperties;

    private volatile ScheduledExecutorService executor;

    public void startMonitoring(Long testRunId) {
        stopMonitoring();
        var factory = Thread.ofVirtual().factory();
        executor = Executors.newSingleThreadScheduledExecutor(factory);
        executor.scheduleAtFixedRate(
                () -> captureSnapshot(testRunId),
                0, POLL_INTERVAL_SECONDS, TimeUnit.SECONDS);
        log.info("Infrastructure monitoring started for test run {}", testRunId);
    }

    public void stopMonitoring() {
        var ex = executor;
        if (ex != null) {
            ex.shutdownNow();
            executor = null;
            log.info("Infrastructure monitoring stopped");
        }
    }

    private void captureSnapshot(Long testRunId) {
        var snapshot = new TestRunSnapshot();
        snapshot.setTestRunId(testRunId);
        snapshot.setSampledAt(Instant.now());
        var captured = 0;

        try {
            var info = ibmMqAdminService.getQueueInfo(monitoringProperties.outboundQueue());
            snapshot.setOutboundQueueDepth(info.currentDepth());
            captured++;
        } catch (Exception e) {
            log.debug("Could not sample outbound queue depth: {}", e.getMessage());
        }

        try {
            var info = ibmMqAdminService.getQueueInfo(monitoringProperties.inboundQueue());
            snapshot.setInboundQueueDepth(info.currentDepth());
            captured++;
        } catch (Exception e) {
            log.debug("Could not sample inbound queue depth: {}", e.getMessage());
        }

        try {
            var lag = kafkaAdminService.getTotalConsumerGroupLag(
                    monitoringProperties.kafkaRequestConsumerGroup());
            snapshot.setKafkaRequestsLag(lag);
            captured++;
        } catch (Exception e) {
            log.debug("Could not sample kafka requests lag: {}", e.getMessage());
        }

        try {
            var lag = kafkaAdminService.getTotalConsumerGroupLag(
                    monitoringProperties.kafkaResponseConsumerGroup());
            snapshot.setKafkaResponsesLag(lag);
            captured++;
        } catch (Exception e) {
            log.debug("Could not sample kafka responses lag: {}", e.getMessage());
        }

        if (captured > 0) {
            snapshotRepository.save(snapshot);
        } else {
            log.debug("Skipping snapshot — no metrics sampled for test run {}", testRunId);
        }
    }
}
