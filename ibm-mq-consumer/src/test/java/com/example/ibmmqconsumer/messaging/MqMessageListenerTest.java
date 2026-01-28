package com.example.ibmmqconsumer.messaging;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import jakarta.jms.Destination;
import jakarta.jms.JMSException;
import jakarta.jms.TextMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.Message;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MqMessageListenerTest {

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

    @Mock
    private TextMessage jmsMessage;

    @Mock
    private Destination replyToDestination;

    private MeterRegistry meterRegistry;
    private MqMessageListener listener;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        listener = new MqMessageListener(kafkaTemplate, meterRegistry, tracer);
        ReflectionTestUtils.setField(listener, "kafkaRequestTopic", "mq-requests");
    }

    @Test
    void onMessageShouldForwardToKafkaWhenReplyToIsSet() throws JMSException {
        setupTracerMocks();
        when(spanContext.getTraceId()).thenReturn("trace-123");
        when(spanContext.getSpanId()).thenReturn("span-456");

        when(jmsMessage.getText()).thenReturn("test message");
        when(jmsMessage.getJMSReplyTo()).thenReturn(replyToDestination);
        when(jmsMessage.getJMSCorrelationID()).thenReturn("corr-123");
        when(jmsMessage.getStringProperty("traceId")).thenReturn("parent-trace");
        when(jmsMessage.getStringProperty("spanId")).thenReturn("parent-span");
        when(replyToDestination.toString()).thenReturn("queue:///DEV.QUEUE.1");

        listener.onMessage(jmsMessage);

        ArgumentCaptor<Message<String>> messageCaptor = ArgumentCaptor.forClass(Message.class);
        verify(kafkaTemplate).send(messageCaptor.capture());

        Message<String> sentMessage = messageCaptor.getValue();
        assertEquals("test message", sentMessage.getPayload());
        assertEquals("mq-requests", sentMessage.getHeaders().get("kafka_topic"));
        assertEquals("queue:///DEV.QUEUE.1", sentMessage.getHeaders().get("mq-reply-to"));
        assertEquals("corr-123", sentMessage.getHeaders().get("correlationId"));
        verify(span).end();
    }

    @Test
    void onMessageShouldDropMessageWhenNoReplyTo() throws JMSException {
        setupTracerMocksWithoutSpanContext();

        when(jmsMessage.getText()).thenReturn("test message");
        when(jmsMessage.getJMSReplyTo()).thenReturn(null);

        listener.onMessage(jmsMessage);

        verify(kafkaTemplate, never()).send(any(Message.class));
        verify(span).end();

        double dropped = meterRegistry.counter("mq.listener.messages.dropped", "listener", "mq-to-kafka").count();
        assertTrue(dropped >= 1.0);
    }

    @Test
    void onMessageShouldRecordExceptionOnError() throws JMSException {
        setupTracerMocks();
        when(spanContext.getTraceId()).thenReturn("trace-123");
        when(spanContext.getSpanId()).thenReturn("span-456");

        when(jmsMessage.getText()).thenReturn("test message");
        when(jmsMessage.getJMSReplyTo()).thenReturn(replyToDestination);
        when(jmsMessage.getJMSCorrelationID()).thenReturn(null);
        when(jmsMessage.getStringProperty("traceId")).thenReturn(null);
        when(jmsMessage.getStringProperty("spanId")).thenReturn(null);
        when(replyToDestination.toString()).thenReturn("DEV.QUEUE.1");

        RuntimeException exception = new RuntimeException("Kafka send failed");
        doThrow(exception).when(kafkaTemplate).send(any(Message.class));

        assertThrows(RuntimeException.class, () -> listener.onMessage(jmsMessage));

        verify(span).recordException(exception);
        verify(span).end();
    }

    @Test
    void countersShouldIncrement() throws JMSException {
        setupTracerMocks();
        when(spanContext.getTraceId()).thenReturn("trace-123");
        when(spanContext.getSpanId()).thenReturn("span-456");

        when(jmsMessage.getText()).thenReturn("test message");
        when(jmsMessage.getJMSReplyTo()).thenReturn(replyToDestination);
        when(jmsMessage.getJMSCorrelationID()).thenReturn(null);
        when(jmsMessage.getStringProperty("traceId")).thenReturn(null);
        when(jmsMessage.getStringProperty("spanId")).thenReturn(null);
        when(replyToDestination.toString()).thenReturn("DEV.QUEUE.1");

        listener.onMessage(jmsMessage);

        double received = meterRegistry.counter("mq.listener.messages.received", "listener", "mq-to-kafka").count();
        double forwarded = meterRegistry.counter("mq.listener.messages.forwarded", "listener", "mq-to-kafka").count();

        assertTrue(received >= 1.0);
        assertTrue(forwarded >= 1.0);
    }

    private void setupTracerMocks() {
        setupTracerMocksWithoutSpanContext();
        when(span.getSpanContext()).thenReturn(spanContext);
    }

    private void setupTracerMocksWithoutSpanContext() {
        when(tracer.spanBuilder(anyString())).thenReturn(spanBuilder);
        when(spanBuilder.setSpanKind(any())).thenReturn(spanBuilder);
        when(spanBuilder.setAttribute(anyString(), anyString())).thenReturn(spanBuilder);
        when(spanBuilder.startSpan()).thenReturn(span);
        when(span.makeCurrent()).thenReturn(scope);
    }
}
