package com.example.kafkaconsumer.config;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Tracer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class TracingConfigTest {

    private TracingConfig tracingConfig;

    @BeforeEach
    void setUp() {
        tracingConfig = new TracingConfig();
        ReflectionTestUtils.setField(tracingConfig, "applicationName", "test-app");
        ReflectionTestUtils.setField(tracingConfig, "tracingUrl", "http://localhost:4318/v1/traces");
    }

    @Test
    void openTelemetryShouldBeCreated() {
        OpenTelemetry openTelemetry = tracingConfig.openTelemetry();

        assertNotNull(openTelemetry);
    }

    @Test
    void tracerShouldBeCreated() {
        OpenTelemetry openTelemetry = tracingConfig.openTelemetry();

        Tracer tracer = tracingConfig.tracer(openTelemetry);

        assertNotNull(tracer);
    }
}
