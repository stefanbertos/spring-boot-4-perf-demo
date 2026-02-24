package com.example.perftester.perf;

import org.springframework.stereotype.Service;

import java.util.concurrent.ThreadLocalRandom;

@Service
public class ThinkTimeCalculator {

    public long nextSleepMs(ThinkTimeConfig config) {
        return switch (config.distribution()) {
            case "UNIFORM" -> ThreadLocalRandom.current().nextLong(
                    config.minMs(), Math.max(config.minMs() + 1, config.maxMs()));
            case "GAUSSIAN" -> {
                long val = Math.round(config.meanMs()
                        + ThreadLocalRandom.current().nextGaussian() * config.stdDevMs());
                yield Math.max(config.minMs(), Math.min(config.maxMs(), val));
            }
            default -> config.minMs();
        };
    }
}
