package com.example.perftester.monitoring;

import com.example.perftester.admin.IbmMqAdminService;
import com.example.perftester.admin.KafkaAdminService;
import com.example.perftester.config.MonitoringProperties;
import com.example.perftester.persistence.TestRunSnapshot;
import com.example.perftester.persistence.TestRunSnapshotRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.awaitility.Awaitility.await;
import static java.time.Duration.ofSeconds;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class InfraSnapshotServiceTest {

    @Mock
    private IbmMqAdminService ibmMqAdminService;

    @Mock
    private KafkaAdminService kafkaAdminService;

    @Mock
    private TestRunSnapshotRepository snapshotRepository;

    private final MonitoringProperties monitoringProperties = new MonitoringProperties(
            "DEV.QUEUE.2", "DEV.QUEUE.1", "ibm-mq-consumer", "ibm-mq-consumer");

    private InfraSnapshotService service;

    @BeforeEach
    void setUp() throws Exception {
        when(ibmMqAdminService.getQueueInfo(anyString()))
                .thenReturn(new IbmMqAdminService.QueueInfo("DEV.QUEUE.2", 0, 5000));
        when(kafkaAdminService.getTotalConsumerGroupLag(anyString())).thenReturn(0L);
        when(snapshotRepository.save(any(TestRunSnapshot.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        service = new InfraSnapshotService(ibmMqAdminService, kafkaAdminService,
                snapshotRepository, monitoringProperties);
    }

    @Test
    void startMonitoringShouldSaveSnapshotsAsync() {
        service.startMonitoring(42L);

        await().atMost(ofSeconds(5))
                .untilAsserted(() -> verify(snapshotRepository, atLeastOnce()).save(any()));

        service.stopMonitoring();
    }

    @Test
    void stopMonitoringWhenNotStartedShouldNotThrow() {
        service.stopMonitoring();
    }

    @Test
    void startMonitoringWhenAlreadyRunningRestartsCleanly() {
        service.startMonitoring(1L);
        service.startMonitoring(2L);

        await().atMost(ofSeconds(5))
                .untilAsserted(() -> verify(snapshotRepository, atLeastOnce()).save(any()));

        service.stopMonitoring();
    }

    @Test
    void captureSnapshotSkipsSaveWhenAllMetricsFail() throws Exception {
        when(ibmMqAdminService.getQueueInfo(anyString())).thenThrow(new RuntimeException("MQ offline"));
        when(kafkaAdminService.getTotalConsumerGroupLag(anyString()))
                .thenThrow(new RuntimeException("Kafka offline"));

        service.startMonitoring(99L);

        await().atMost(ofSeconds(3)).pollDelay(ofSeconds(1)).untilAsserted(() ->
                verify(snapshotRepository, never()).save(any()));

        service.stopMonitoring();
    }
}
