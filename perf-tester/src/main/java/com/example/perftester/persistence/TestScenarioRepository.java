package com.example.perftester.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TestScenarioRepository extends JpaRepository<TestScenario, Long> {

    List<TestScenario> findByScheduledEnabledTrue();
}
