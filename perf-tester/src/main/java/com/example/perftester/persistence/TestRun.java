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
@Table(name = "test_run")
@Getter
@Setter
public class TestRun {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "test_run_seq")
    @SequenceGenerator(name = "test_run_seq", sequenceName = "test_run_seq", allocationSize = 1)
    private Long id;

    @Column(name = "test_run_id", nullable = false, length = 36)
    private String testRunId;

    @Column(name = "test_id")
    private String testId;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "message_count", nullable = false)
    private int messageCount;

    @Column(name = "completed_count", nullable = false)
    private long completedCount;

    @Column(name = "tps")
    private Double tps;

    @Column(name = "avg_latency_ms")
    private Double avgLatencyMs;

    @Column(name = "min_latency_ms")
    private Double minLatencyMs;

    @Column(name = "max_latency_ms")
    private Double maxLatencyMs;

    @Column(name = "p50_latency_ms")
    private Double p50LatencyMs;

    @Column(name = "p90_latency_ms")
    private Double p90LatencyMs;

    @Column(name = "p95_latency_ms")
    private Double p95LatencyMs;

    @Column(name = "p99_latency_ms")
    private Double p99LatencyMs;

    @Column(name = "timeout_count", nullable = false)
    private long timeoutCount;

    @Column(name = "test_type", length = 20)
    private String testType;

    @Column(name = "threshold_status", length = 20)
    private String thresholdStatus;

    @Column(name = "threshold_results", columnDefinition = "text")
    private String thresholdResults;

    @Column(name = "duration_ms")
    private Long durationMs;

    @Column(name = "zip_file_path", length = 1000)
    private String zipFilePath;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }
}
