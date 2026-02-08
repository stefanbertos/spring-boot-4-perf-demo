package com.example.kafkaconsumer.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class TracingConfigTest {

    private TracingConfig tracingConfig;

    @BeforeEach
    void setUp() {
        tracingConfig = new TracingConfig("test-app", "http://localhost:4318/v1/traces");
    }

    @Test
    void openTelemetryShouldBeCreated() {
        var openTelemetry = tracingConfig.openTelemetry();

        assertNotNull(openTelemetry);
    }

    @Test
    void tracerShouldBeCreated() {
        var openTelemetry = tracingConfig.openTelemetry();

        var tracer = tracingConfig.tracer(openTelemetry);

        assertNotNull(tracer);
    }
}
