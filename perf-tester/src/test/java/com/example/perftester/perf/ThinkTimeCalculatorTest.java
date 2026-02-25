package com.example.perftester.perf;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ThinkTimeCalculatorTest {

    private final ThinkTimeCalculator calculator = new ThinkTimeCalculator();

    @Test
    void constantDistributionReturnsMinMs() {
        var config = new ThinkTimeConfig("CONSTANT", 50, 200, 100.0, 20.0);
        assertEquals(50, calculator.nextSleepMs(config));
    }

    @Test
    void uniformDistributionReturnsValueInRange() {
        var config = new ThinkTimeConfig("UNIFORM", 10, 100, 0, 0);
        for (int i = 0; i < 20; i++) {
            long sleep = calculator.nextSleepMs(config);
            assertTrue(sleep >= 10 && sleep < 100, "Expected [10,100) but got " + sleep);
        }
    }

    @Test
    void gaussianDistributionReturnsClamped() {
        var config = new ThinkTimeConfig("GAUSSIAN", 0, 500, 100.0, 30.0);
        for (int i = 0; i < 20; i++) {
            long sleep = calculator.nextSleepMs(config);
            assertTrue(sleep >= 0 && sleep <= 500, "Expected [0,500] but got " + sleep);
        }
    }

    @Test
    void unknownDistributionFallsBackToConstant() {
        var config = new ThinkTimeConfig("POISSON", 25, 200, 100.0, 10.0);
        assertEquals(25, calculator.nextSleepMs(config));
    }
}
