package com.example.perftester.health;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface HealthCheckConfigRepository extends JpaRepository<HealthCheckConfig, String> {
}
