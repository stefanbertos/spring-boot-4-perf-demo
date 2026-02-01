package com.example.perftester.persistence.repository;

import com.example.perftester.persistence.entity.TestRun;
import com.example.perftester.persistence.entity.TestRun.TestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface TestRunRepository extends JpaRepository<TestRun, Long> {

    Optional<TestRun> findByTestId(String testId);

    List<TestRun> findByStatus(TestStatus status);

    List<TestRun> findByStartTimeBetweenOrderByStartTimeDesc(Instant startTime, Instant endTime);

    @Query("SELECT t FROM TestRun t ORDER BY t.startTime DESC")
    List<TestRun> findRecentTestRuns();

    @Query("SELECT t FROM TestRun t WHERE t.status = 'RUNNING' ORDER BY t.startTime DESC")
    List<TestRun> findRunningTests();
}
