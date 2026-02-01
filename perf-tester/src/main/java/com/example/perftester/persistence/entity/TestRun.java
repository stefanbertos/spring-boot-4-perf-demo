package com.example.perftester.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "PERF_TEST_RUN")
@Getter
@Setter
@NoArgsConstructor
public class TestRun {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "test_run_seq")
    @SequenceGenerator(name = "test_run_seq", sequenceName = "PERF_TEST_RUN_SEQ", allocationSize = 1)
    private Long id;

    @Column(name = "TEST_ID", length = 100)
    private String testId;

    @Enumerated(EnumType.STRING)
    @Column(name = "STATUS", nullable = false, length = 20)
    private TestStatus status;

    @Column(name = "MESSAGE_COUNT", nullable = false)
    private Integer messageCount;

    @Column(name = "COMPLETED_COUNT")
    private Integer completedCount = 0;

    @Column(name = "FAILED_COUNT")
    private Integer failedCount = 0;

    @Column(name = "START_TIME", nullable = false)
    private Instant startTime;

    @Column(name = "END_TIME")
    private Instant endTime;

    @Column(name = "DURATION_MS")
    private Long durationMs;

    @Column(name = "TPS", precision = 10, scale = 2)
    private BigDecimal tps;

    @Column(name = "AVG_LATENCY_MS", precision = 10, scale = 2)
    private BigDecimal avgLatencyMs;

    @Column(name = "MIN_LATENCY_MS", precision = 10, scale = 2)
    private BigDecimal minLatencyMs;

    @Column(name = "MAX_LATENCY_MS", precision = 10, scale = 2)
    private BigDecimal maxLatencyMs;

    @Column(name = "P50_LATENCY_MS", precision = 10, scale = 2)
    private BigDecimal p50LatencyMs;

    @Column(name = "P95_LATENCY_MS", precision = 10, scale = 2)
    private BigDecimal p95LatencyMs;

    @Column(name = "P99_LATENCY_MS", precision = 10, scale = 2)
    private BigDecimal p99LatencyMs;

    @Column(name = "CREATED_AT", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "UPDATED_AT", nullable = false)
    private Instant updatedAt;

    public TestRun(String testId, int messageCount) {
        this.testId = testId;
        this.messageCount = messageCount;
        this.status = TestStatus.RUNNING;
        this.startTime = Instant.now();
    }

    @PrePersist
    protected void onCreate() {
        var now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    public enum TestStatus {
        RUNNING,
        COMPLETED,
        FAILED,
        TIMEOUT
    }
}
