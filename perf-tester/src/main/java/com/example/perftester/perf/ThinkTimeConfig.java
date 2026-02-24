package com.example.perftester.perf;

public record ThinkTimeConfig(String distribution, int minMs, int maxMs,
                               double meanMs, double stdDevMs) {
    // distribution: CONSTANT | UNIFORM | GAUSSIAN
}
