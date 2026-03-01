package com.example.perftester.persistence;

import com.example.perftester.perf.ThinkTimeConfig;
import com.example.perftester.perf.ThresholdDef;

import java.util.List;

public record TestScenarioDetail(Long id, String name, int count, List<ScenarioEntryDto> entries,
                                 boolean scheduledEnabled, String scheduledTime,
                                 int warmupCount, String testType, Long infraProfileId,
                                 ThinkTimeConfig thinkTime, List<ThresholdDef> thresholds,
                                 String createdAt, String updatedAt) {
}
