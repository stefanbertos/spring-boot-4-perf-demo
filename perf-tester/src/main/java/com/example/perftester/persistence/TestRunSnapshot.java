package com.example.perftester.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "test_run_snapshot")
@Getter
@Setter
public class TestRunSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "test_run_snapshot_seq")
    @SequenceGenerator(name = "test_run_snapshot_seq", sequenceName = "test_run_snapshot_seq",
            allocationSize = 1)
    private Long id;

    @Column(name = "test_run_id", nullable = false)
    private Long testRunId;

    @Column(name = "sampled_at", nullable = false)
    private Instant sampledAt;

    @Column(name = "outbound_queue_depth")
    private Integer outboundQueueDepth;

    @Column(name = "inbound_queue_depth")
    private Integer inboundQueueDepth;

    @Column(name = "kafka_requests_lag")
    private Long kafkaRequestsLag;

    @Column(name = "kafka_responses_lag")
    private Long kafkaResponsesLag;
}
