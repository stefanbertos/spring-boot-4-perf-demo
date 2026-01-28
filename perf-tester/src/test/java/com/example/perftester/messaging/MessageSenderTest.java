package com.example.perftester.messaging;

import com.example.perftester.perf.PerformanceTracker;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.Queue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessagePostProcessor;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MessageSenderTest {

    @Mock
    private JmsTemplate jmsTemplate;

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
    private SpanContext spanContext;

    @Mock
    private Message jmsMessage;

    private MessageSender messageSender;

    @BeforeEach
    void setUp() throws JMSException {
        messageSender = new MessageSender(jmsTemplate, "DEV.QUEUE.2", "DEV.QUEUE.1", performanceTracker, tracer);
    }

    @Test
    void sendMessageShouldSendToOutboundQueue() throws JMSException {
        setupTracerMocks();
        when(spanContext.getTraceId()).thenReturn("trace-123");
        when(spanContext.getSpanId()).thenReturn("span-456");

        messageSender.sendMessage("test payload");

        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<MessagePostProcessor> processorCaptor = ArgumentCaptor.forClass(MessagePostProcessor.class);
        verify(jmsTemplate).convertAndSend(eq("DEV.QUEUE.2"), messageCaptor.capture(), processorCaptor.capture());

        String sentMessage = messageCaptor.getValue();
        assertTrue(sentMessage.contains("|test payload"));
        verify(performanceTracker).recordSend(anyString());
        verify(span).end();

        // Invoke the lambda to cover the MessagePostProcessor code
        MessagePostProcessor processor = processorCaptor.getValue();
        Message result = processor.postProcessMessage(jmsMessage);
        assertNotNull(result);
        verify(jmsMessage).setJMSReplyTo(any(Queue.class));
        verify(jmsMessage).setStringProperty("traceId", "trace-123");
        verify(jmsMessage).setStringProperty("spanId", "span-456");
        verify(jmsMessage).setJMSCorrelationID(anyString());
    }

    @Test
    void sendMessageShouldRecordExceptionOnError() {
        setupTracerMocks();
        when(spanContext.getTraceId()).thenReturn("trace-123");
        when(spanContext.getSpanId()).thenReturn("span-456");

        RuntimeException exception = new RuntimeException("JMS send failed");
        doThrow(exception).when(jmsTemplate).convertAndSend(anyString(), anyString(), any(MessagePostProcessor.class));

        assertThrows(RuntimeException.class, () -> messageSender.sendMessage("test payload"));

        verify(span).recordException(exception);
        verify(span).end();
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
