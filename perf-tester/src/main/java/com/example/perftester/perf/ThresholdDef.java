package com.example.perftester.perf;

public record ThresholdDef(String metric, String operator, double value) {
    // metric: TPS | AVG_LATENCY | P50 | P90 | P95 | P99
    // operator: LT | LTE | GT | GTE
}
