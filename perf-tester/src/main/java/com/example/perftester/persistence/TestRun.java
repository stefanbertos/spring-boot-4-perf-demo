package com.example.perftester.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "TEST_RUN")
@Getter
@Setter
public class TestRun {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "test_run_seq")
    @SequenceGenerator(name = "test_run_seq", sequenceName = "TEST_RUN_SEQ", allocationSize = 1)
    private Long id;

    @Column(name = "TEST_RUN_ID", nullable = false, length = 36)
    private String testRunId;

    @Column(name = "TEST_ID")
    private String testId;

    @Column(name = "STATUS", nullable = false, length = 20)
    private String status;

    @Column(name = "MESSAGE_COUNT", nullable = false)
    private int messageCount;

    @Column(name = "COMPLETED_COUNT", nullable = false)
    private long completedCount;

    @Column(name = "TPS")
    private Double tps;

    @Column(name = "AVG_LATENCY_MS")
    private Double avgLatencyMs;

    @Column(name = "MIN_LATENCY_MS")
    private Double minLatencyMs;

    @Column(name = "MAX_LATENCY_MS")
    private Double maxLatencyMs;

    @Column(name = "DURATION_MS")
    private Long durationMs;

    @Column(name = "ZIP_FILE_PATH", length = 1000)
    private String zipFilePath;

    @Column(name = "STARTED_AT", nullable = false)
    private Instant startedAt;

    @Column(name = "COMPLETED_AT")
    private Instant completedAt;

    @Column(name = "CREATED_AT", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }
}
