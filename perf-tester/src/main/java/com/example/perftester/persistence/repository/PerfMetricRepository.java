package com.example.perftester.persistence.repository;

import com.example.perftester.persistence.entity.PerfMetric;
import com.example.perftester.persistence.entity.PerfMetric.MetricType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface PerfMetricRepository extends JpaRepository<PerfMetric, Long> {

    List<PerfMetric> findByTestRunId(Long testRunId);

    List<PerfMetric> findByTestRunIdAndMetricName(Long testRunId, String metricName);

    List<PerfMetric> findByTestRunIdAndMetricType(Long testRunId, MetricType metricType);

    @Query("SELECT m FROM PerfMetric m WHERE m.testRun.id = :testRunId " +
           "AND m.collectedAt BETWEEN :startTime AND :endTime ORDER BY m.collectedAt")
    List<PerfMetric> findByTestRunIdAndTimeRange(
            @Param("testRunId") Long testRunId,
            @Param("startTime") Instant startTime,
            @Param("endTime") Instant endTime);

    @Query("SELECT DISTINCT m.metricName FROM PerfMetric m WHERE m.testRun.id = :testRunId")
    List<String> findDistinctMetricNamesByTestRunId(@Param("testRunId") Long testRunId);
}
