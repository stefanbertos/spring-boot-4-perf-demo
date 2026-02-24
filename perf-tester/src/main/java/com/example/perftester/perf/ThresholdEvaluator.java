package com.example.perftester.perf;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ThresholdEvaluator {

    public List<ThresholdResult> evaluate(List<ThresholdDef> defs, PerfTestResult result) {
        return defs.stream()
                .map(def -> evaluateOne(def, result))
                .toList();
    }

    private ThresholdResult evaluateOne(ThresholdDef def, PerfTestResult result) {
        double actual = switch (def.metric()) {
            case "TPS" -> result.tps();
            case "AVG_LATENCY" -> result.avgLatencyMs();
            case "P50" -> result.p50LatencyMs();
            case "P90" -> result.p90LatencyMs();
            case "P95" -> result.p95LatencyMs();
            case "P99" -> result.p99LatencyMs();
            default -> 0;
        };
        boolean passed = switch (def.operator()) {
            case "LT" -> actual < def.value();
            case "LTE" -> actual <= def.value();
            case "GT" -> actual > def.value();
            case "GTE" -> actual >= def.value();
            default -> false;
        };
        return new ThresholdResult(def.metric(), def.operator(), def.value(), actual, passed);
    }
}
