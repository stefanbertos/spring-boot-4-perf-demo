package com.example.kafkaconsumer.messaging;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.Message;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KafkaRequestListenerTest {

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @Mock
    private Tracer tracer;

    @Mock
    private SpanBuilder spanBuilder;

    @Mock
    private Span span;

    @Mock
    private Scope scope;

    @Mock
    private SpanContext spanContext;

    private MeterRegistry meterRegistry;
    private KafkaRequestListener listener;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        listener = new KafkaRequestListener(kafkaTemplate, meterRegistry, tracer);
        ReflectionTestUtils.setField(listener, "kafkaResponseTopic", "mq-responses");
    }

    @Test
    void onMessageShouldProcessAndSendResponse() {
        setupTracerMocks();
        when(spanContext.getTraceId()).thenReturn("trace-123");
        when(spanContext.getSpanId()).thenReturn("span-456");

        RecordHeaders headers = new RecordHeaders();
        headers.add("mq-reply-to", "queue:///DEV.QUEUE.1".getBytes(StandardCharsets.UTF_8));
        headers.add("correlationId", "corr-123".getBytes(StandardCharsets.UTF_8));
        headers.add("traceId", "parent-trace".getBytes(StandardCharsets.UTF_8));
        headers.add("spanId", "parent-span".getBytes(StandardCharsets.UTF_8));

        ConsumerRecord<String, String> record = new ConsumerRecord<>(
                "mq-requests", 0, 0, "key", "test message");
        record.headers().add("mq-reply-to", "queue:///DEV.QUEUE.1".getBytes(StandardCharsets.UTF_8));
        record.headers().add("correlationId", "corr-123".getBytes(StandardCharsets.UTF_8));
        record.headers().add("traceId", "parent-trace".getBytes(StandardCharsets.UTF_8));
        record.headers().add("spanId", "parent-span".getBytes(StandardCharsets.UTF_8));

        listener.onMessage(record);

        ArgumentCaptor<Message<String>> messageCaptor = ArgumentCaptor.forClass(Message.class);
        verify(kafkaTemplate).send(messageCaptor.capture());

        Message<String> sentMessage = messageCaptor.getValue();
        assertEquals("test message processed", sentMessage.getPayload());
        assertEquals("mq-responses", sentMessage.getHeaders().get("kafka_topic"));
        assertEquals("queue:///DEV.QUEUE.1", sentMessage.getHeaders().get("mq-reply-to"));
        verify(span).end();
    }

    @Test
    void onMessageShouldHandleNullHeaders() {
        setupTracerMocks();
        when(spanContext.getTraceId()).thenReturn("trace-123");
        when(spanContext.getSpanId()).thenReturn("span-456");

        ConsumerRecord<String, String> record = new ConsumerRecord<>(
                "mq-requests", 0, 0, "key", "test message");

        listener.onMessage(record);

        ArgumentCaptor<Message<String>> messageCaptor = ArgumentCaptor.forClass(Message.class);
        verify(kafkaTemplate).send(messageCaptor.capture());

        Message<String> sentMessage = messageCaptor.getValue();
        assertEquals("test message processed", sentMessage.getPayload());
        verify(span).end();
    }

    @Test
    void onMessageShouldRecordExceptionOnError() {
        setupTracerMocks();
        when(spanContext.getTraceId()).thenReturn("trace-123");
        when(spanContext.getSpanId()).thenReturn("span-456");

        RuntimeException exception = new RuntimeException("Send failed");
        doThrow(exception).when(kafkaTemplate).send(any(Message.class));

        ConsumerRecord<String, String> record = new ConsumerRecord<>(
                "mq-requests", 0, 0, "key", "test message");

        try {
            listener.onMessage(record);
        } catch (RuntimeException e) {
            assertEquals("Send failed", e.getMessage());
        }

        verify(span).recordException(exception);
        verify(span).end();
    }

    @Test
    void countersShouldIncrement() {
        setupTracerMocks();
        when(spanContext.getTraceId()).thenReturn("trace-123");
        when(spanContext.getSpanId()).thenReturn("span-456");

        ConsumerRecord<String, String> record = new ConsumerRecord<>(
                "mq-requests", 0, 0, "key", "test message");

        listener.onMessage(record);

        double received = meterRegistry.counter("kafka.request.messages.received", "listener", "kafka-processor").count();
        double processed = meterRegistry.counter("kafka.request.messages.processed", "listener", "kafka-processor").count();

        assertTrue(received >= 1.0);
        assertTrue(processed >= 1.0);
    }

    private void setupTracerMocks() {
        when(tracer.spanBuilder(anyString())).thenReturn(spanBuilder);
        when(spanBuilder.setSpanKind(any())).thenReturn(spanBuilder);
        when(spanBuilder.setAttribute(anyString(), anyString())).thenReturn(spanBuilder);
        when(spanBuilder.startSpan()).thenReturn(span);
        when(span.makeCurrent()).thenReturn(scope);
        when(span.getSpanContext()).thenReturn(spanContext);
    }
}
