package com.example.perftester.messaging;

import com.example.perftester.perf.PerformanceTracker;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import jakarta.jms.JMSException;
import jakarta.jms.TextMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MessageListenerTest {

    @Mock
    private PerformanceTracker performanceTracker;

    @Mock
    private Tracer tracer;

    @Mock
    private SpanBuilder spanBuilder;

    @Mock
    private Span span;

    @Mock
    private Scope scope;

    @Mock
    private TextMessage jmsMessage;

    private MessageListener listener;

    @BeforeEach
    void setUp() {
        listener = new MessageListener(performanceTracker, tracer);
    }

    @Test
    void receiveMessageShouldRecordReceiveForValidMessage() throws JMSException {
        setupTracerMocks();

        when(jmsMessage.getText()).thenReturn("msg-123|test payload processed");
        when(jmsMessage.getStringProperty("traceId")).thenReturn("trace-123");
        when(jmsMessage.getJMSCorrelationID()).thenReturn("corr-123");

        listener.receiveMessage(jmsMessage);

        verify(performanceTracker).recordReceive("msg-123");
        verify(span).end();
    }

    @Test
    void receiveMessageShouldHandleNullTraceId() throws JMSException {
        setupTracerMocks();

        when(jmsMessage.getText()).thenReturn("msg-123|test payload");
        when(jmsMessage.getStringProperty("traceId")).thenReturn(null);
        when(jmsMessage.getJMSCorrelationID()).thenReturn(null);

        listener.receiveMessage(jmsMessage);

        verify(performanceTracker).recordReceive("msg-123");
        verify(span).end();
    }

    @Test
    void receiveMessageShouldNotRecordForInvalidMessage() throws JMSException {
        setupTracerMocks();

        when(jmsMessage.getText()).thenReturn("invalid message without separator");
        when(jmsMessage.getStringProperty("traceId")).thenReturn(null);
        when(jmsMessage.getJMSCorrelationID()).thenReturn(null);

        listener.receiveMessage(jmsMessage);

        verify(performanceTracker, never()).recordReceive(anyString());
        verify(span).end();
    }

    @Test
    void receiveMessageShouldRecordExceptionOnError() throws JMSException {
        setupTracerMocks();

        when(jmsMessage.getText()).thenReturn("msg-123|test payload");
        when(jmsMessage.getStringProperty("traceId")).thenReturn(null);
        when(jmsMessage.getJMSCorrelationID()).thenReturn(null);

        RuntimeException exception = new RuntimeException("Tracker failed");
        doThrow(exception).when(performanceTracker).recordReceive(anyString());

        assertThrows(RuntimeException.class, () -> listener.receiveMessage(jmsMessage));

        verify(span).recordException(exception);
        verify(span).end();
    }

    private void setupTracerMocks() {
        when(tracer.spanBuilder(anyString())).thenReturn(spanBuilder);
        when(spanBuilder.setSpanKind(any())).thenReturn(spanBuilder);
        when(spanBuilder.setAttribute(anyString(), anyString())).thenReturn(spanBuilder);
        when(spanBuilder.startSpan()).thenReturn(span);
        when(span.makeCurrent()).thenReturn(scope);
    }
}
