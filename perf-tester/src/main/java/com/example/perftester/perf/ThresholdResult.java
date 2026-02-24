package com.example.perftester.perf;

public record ThresholdResult(String metric, String operator, double threshold,
                              double actual, boolean passed) {
}
