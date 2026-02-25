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
import static org.mockito.Mockito.atLeastOnce;
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
    void setUp() {
        service = new InfraSnapshotService(ibmMqAdminService, kafkaAdminService,
                snapshotRepository, monitoringProperties);
    }

    @Test
    void startMonitoringShouldSaveSnapshotsAsync() {
        when(snapshotRepository.save(any(TestRunSnapshot.class)))
                .thenAnswer(inv -> inv.getArgument(0));

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
        when(snapshotRepository.save(any(TestRunSnapshot.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        service.startMonitoring(1L);
        service.startMonitoring(2L);

        await().atMost(ofSeconds(5))
                .untilAsserted(() -> verify(snapshotRepository, atLeastOnce()).save(any()));

        service.stopMonitoring();
    }

    @Test
    void captureSnapshotContinuesWhenDepsThrow() throws Exception {
        when(ibmMqAdminService.getQueueInfo(any())).thenThrow(new RuntimeException("MQ offline"));
        when(kafkaAdminService.getTotalConsumerGroupLag(any())).thenThrow(new RuntimeException("Kafka offline"));
        when(snapshotRepository.save(any(TestRunSnapshot.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        service.startMonitoring(99L);

        await().atMost(ofSeconds(5))
                .untilAsserted(() -> verify(snapshotRepository, atLeastOnce()).save(any()));

        service.stopMonitoring();
    }
}
