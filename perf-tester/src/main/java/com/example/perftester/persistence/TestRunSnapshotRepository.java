package com.example.perftester.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TestRunSnapshotRepository extends JpaRepository<TestRunSnapshot, Long> {

    List<TestRunSnapshot> findByTestRunIdOrderBySampledAtAsc(Long testRunId);
}
