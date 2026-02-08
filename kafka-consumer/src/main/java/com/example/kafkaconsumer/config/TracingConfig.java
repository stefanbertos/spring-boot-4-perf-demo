package com.example.kafkaconsumer.config;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.exporter.otlp.http.trace.OtlpHttpSpanExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.semconv.ServiceAttributes;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TracingConfig {

    private final String applicationName;
    private final String tracingUrl;

    public TracingConfig(@Value("${spring.application.name}") String applicationName,
                         @Value("${tracing.url:http://localhost:4318/v1/traces}") String tracingUrl) {
        this.applicationName = applicationName;
        this.tracingUrl = tracingUrl;
    }

    @Bean
    public OpenTelemetry openTelemetry() {
        var resource = Resource.getDefault()
                .merge(Resource.builder()
                        .put(ServiceAttributes.SERVICE_NAME, applicationName)
                        .build());

        var spanExporter = OtlpHttpSpanExporter.builder()
                .setEndpoint(tracingUrl)
                .build();

        var tracerProvider = SdkTracerProvider.builder()
                .addSpanProcessor(BatchSpanProcessor.builder(spanExporter).build())
                .setResource(resource)
                .build();

        return OpenTelemetrySdk.builder()
                .setTracerProvider(tracerProvider)
                .setPropagators(ContextPropagators.create(W3CTraceContextPropagator.getInstance()))
                .build();
    }

    @Bean
    public Tracer tracer(OpenTelemetry openTelemetry) {
        return openTelemetry.getTracer(applicationName);
    }
}
