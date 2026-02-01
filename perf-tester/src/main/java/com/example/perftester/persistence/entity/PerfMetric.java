package com.example.perftester.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "PERF_METRIC")
@Getter
@Setter
@NoArgsConstructor
public class PerfMetric {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "perf_metric_seq")
    @SequenceGenerator(name = "perf_metric_seq", sequenceName = "PERF_METRIC_SEQ", allocationSize = 1)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "TEST_RUN_ID", nullable = false)
    private TestRun testRun;

    @Column(name = "METRIC_NAME", nullable = false, length = 200)
    private String metricName;

    @Enumerated(EnumType.STRING)
    @Column(name = "METRIC_TYPE", nullable = false, length = 50)
    private MetricType metricType;

    @Column(name = "METRIC_VALUE", nullable = false, precision = 20, scale = 6)
    private BigDecimal metricValue;

    @Column(name = "METRIC_UNIT", length = 50)
    private String metricUnit;

    @Column(name = "TAGS", length = 2000)
    private String tags;

    @Column(name = "COLLECTED_AT", nullable = false)
    private Instant collectedAt;

    @Column(name = "CREATED_AT", nullable = false, updatable = false)
    private Instant createdAt;

    public PerfMetric(TestRun testRun, String metricName, MetricType metricType,
                      BigDecimal metricValue, String metricUnit) {
        this.testRun = testRun;
        this.metricName = metricName;
        this.metricType = metricType;
        this.metricValue = metricValue;
        this.metricUnit = metricUnit;
        this.collectedAt = Instant.now();
    }

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }

    public enum MetricType {
        COUNTER,
        GAUGE,
        TIMER,
        HISTOGRAM
    }
}
