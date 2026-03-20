package com.example.perftester.persistence;

import com.example.perftester.perf.ThinkTimeConfig;
import com.example.perftester.perf.ThresholdDef;

import java.util.List;

public record ScenarioExport(
        String version,
        String name,
        int count,
        boolean scheduledEnabled,
        String scheduledTime,
        int warmupCount,
        String testType,
        ThinkTimeConfig thinkTime,
        List<ThresholdDef> thresholds,
        List<ScenarioExportEntry> entries) {
}
