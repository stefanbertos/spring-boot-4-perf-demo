package com.example.perftester.loki;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LokiServiceLabelRepository extends JpaRepository<LokiServiceLabel, String> {
}
