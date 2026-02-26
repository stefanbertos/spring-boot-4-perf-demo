package com.example.perftester.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ScenarioTestCaseRepository extends JpaRepository<ScenarioTestCase, Long> {
}
