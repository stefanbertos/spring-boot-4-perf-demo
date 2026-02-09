package com.example.perftester.messaging;

import com.example.perftester.perf.PerformanceTracker;
import jakarta.jms.JMSException;
import jakarta.jms.TextMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;
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
    private TextMessage jmsMessage;

    private MessageListener listener;

    @BeforeEach
    void setUp() {
        listener = new MessageListener(performanceTracker);
    }

    @Test
    void receiveMessageShouldRecordReceiveForValidMessage() throws JMSException {
        when(jmsMessage.getText()).thenReturn("msg-123|test payload processed");
        when(jmsMessage.getJMSCorrelationID()).thenReturn("corr-123");

        listener.receiveMessage(jmsMessage);

        verify(performanceTracker).recordReceive("msg-123");
    }

    @Test
    void receiveMessageShouldHandleNullTraceId() throws JMSException {
        when(jmsMessage.getText()).thenReturn("msg-123|test payload");
        when(jmsMessage.getJMSCorrelationID()).thenReturn(null);

        listener.receiveMessage(jmsMessage);

        verify(performanceTracker).recordReceive("msg-123");
    }

    @Test
    void receiveMessageShouldNotRecordForInvalidMessage() throws JMSException {
        when(jmsMessage.getText()).thenReturn("invalid message without separator");
        when(jmsMessage.getJMSCorrelationID()).thenReturn(null);

        listener.receiveMessage(jmsMessage);

        verify(performanceTracker, never()).recordReceive(anyString());
    }

    @Test
    void receiveMessageShouldRecordExceptionOnError() throws JMSException {
        when(jmsMessage.getText()).thenReturn("msg-123|test payload");
        when(jmsMessage.getJMSCorrelationID()).thenReturn(null);

        RuntimeException exception = new RuntimeException("Tracker failed");
        doThrow(exception).when(performanceTracker).recordReceive(anyString());

        assertThrows(RuntimeException.class, () -> listener.receiveMessage(jmsMessage));
    }
}
