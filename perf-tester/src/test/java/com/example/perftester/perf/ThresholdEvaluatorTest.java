package com.example.perftester.perf;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ThresholdEvaluatorTest {

    private final ThresholdEvaluator evaluator = new ThresholdEvaluator();

    private PerfTestResult resultWith(double tps, double avgLatency, double p50, double p90, double p95, double p99) {
        return new PerfTestResult(100, 0, 10.0, tps, avgLatency, 1.0, 200.0)
                .withPercentiles(0, p50, 0, p90, p95, p99);
    }

    @Test
    void evaluateShouldReturnEmptyForNoDefs() {
        var result = resultWith(100.0, 50.0, 80.0, 99.0, 0.0, 0.0);
        assertTrue(evaluator.evaluate(List.of(), result).isEmpty());
    }

    @Test
    void tpsGteShouldPassWhenAboveThreshold() {
        var def = new ThresholdDef("TPS", "GTE", 80.0);
        var result = resultWith(100.0, 25.0, 75.0, 80.0, 90.0, 99.0);
        var results = evaluator.evaluate(List.of(def), result);
        assertEquals(1, results.size());
        assertTrue(results.get(0).passed());
        assertEquals(100.0, results.get(0).actual());
    }

    @Test
    void tpsGteShouldFailWhenBelowThreshold() {
        var def = new ThresholdDef("TPS", "GTE", 150.0);
        var result = resultWith(100.0, 50.0, 80.0, 99.0, 0.0, 0.0);
        assertFalse(evaluator.evaluate(List.of(def), result).get(0).passed());
    }

    @Test
    void avgLatencyLteShouldPassWhenBelow() {
        var def = new ThresholdDef("AVG_LATENCY", "LTE", 100.0);
        var result = resultWith(100.0, 50.0, 80.0, 99.0, 0.0, 0.0);
        assertTrue(evaluator.evaluate(List.of(def), result).get(0).passed());
    }

    @Test
    void avgLatencyLteShouldFailWhenAbove() {
        var def = new ThresholdDef("AVG_LATENCY", "LTE", 40.0);
        var result = resultWith(100.0, 50.0, 80.0, 99.0, 0.0, 0.0);
        assertFalse(evaluator.evaluate(List.of(def), result).get(0).passed());
    }

    @Test
    void p50LtShouldPass() {
        var def = new ThresholdDef("P50", "LT", 50.0);
        var result = resultWith(100.0, 50.0, 45.0, 99.0, 0.0, 0.0);
        assertTrue(evaluator.evaluate(List.of(def), result).get(0).passed());
    }

    @Test
    void p90GtShouldPass() {
        var def = new ThresholdDef("P90", "GT", 70.0);
        var result = resultWith(100.0, 50.0, 80.0, 99.0, 0.0, 0.0);
        assertTrue(evaluator.evaluate(List.of(def), result).get(0).passed());
    }

    @Test
    void p95EvaluatesCorrectly() {
        var def = new ThresholdDef("P95", "LTE", 90.0);
        var result = resultWith(100.0, 50.0, 80.0, 99.0, 0.0, 0.0);
        assertTrue(evaluator.evaluate(List.of(def), result).get(0).passed());
    }

    @Test
    void p99EvaluatesCorrectly() {
        var def = new ThresholdDef("P99", "LT", 100.0);
        var result = resultWith(100.0, 50.0, 80.0, 99.0, 0.0, 0.0);
        assertTrue(evaluator.evaluate(List.of(def), result).get(0).passed());
    }

    @Test
    void unknownOperatorReturnsFail() {
        var def = new ThresholdDef("TPS", "CONTAINS", 50.0);
        var result = resultWith(100.0, 50.0, 80.0, 99.0, 0.0, 0.0);
        assertFalse(evaluator.evaluate(List.of(def), result).get(0).passed());
    }

    @Test
    void unknownMetricActualIsZero() {
        var def = new ThresholdDef("UNKNOWN_METRIC", "GT", 0.1);
        var result = resultWith(100.0, 50.0, 80.0, 99.0, 0.0, 0.0);
        assertFalse(evaluator.evaluate(List.of(def), result).get(0).passed());
    }

    @Test
    void multipleDefsAreAllEvaluated() {
        var defs = List.of(
                new ThresholdDef("TPS", "GTE", 80.0),
                new ThresholdDef("P95", "LTE", 100.0)
        );
        var result = resultWith(100.0, 50.0, 80.0, 99.0, 0.0, 0.0);
        var results = evaluator.evaluate(defs, result);
        assertEquals(2, results.size());
        assertTrue(results.get(0).passed());
        assertTrue(results.get(1).passed());
    }
}
