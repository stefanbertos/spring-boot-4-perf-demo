package com.example.ibmmqconsumer.config;

import io.micrometer.core.aop.TimedAspect;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class MetricsConfigTest {

    @Test
    void timedAspectShouldBeCreated() {
        MetricsConfig config = new MetricsConfig();
        MeterRegistry meterRegistry = new SimpleMeterRegistry();

        TimedAspect timedAspect = config.timedAspect(meterRegistry);

        assertNotNull(timedAspect);
    }
}
