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
        when(jmsMessage.getJMSCorrelationID()).thenReturn("corr-123");

        listener.receiveMessage(jmsMessage);

        verify(performanceTracker).recordReceive("corr-123");
    }

    @Test
    void receiveMessageShouldNotRecordWhenCorrelationIdIsNull() throws JMSException {
        when(jmsMessage.getJMSCorrelationID()).thenReturn(null);
        when(jmsMessage.getText()).thenReturn("some body without correlation");

        listener.receiveMessage(jmsMessage);

        verify(performanceTracker, never()).recordReceive(anyString());
    }

    @Test
    void receiveMessageShouldNotRecordForMessageWithoutCorrelationId() throws JMSException {
        when(jmsMessage.getJMSCorrelationID()).thenReturn(null);
        when(jmsMessage.getText()).thenReturn("invalid message without separator");

        listener.receiveMessage(jmsMessage);

        verify(performanceTracker, never()).recordReceive(anyString());
    }

    @Test
    void receiveMessageShouldRecordExceptionOnError() throws JMSException {
        when(jmsMessage.getJMSCorrelationID()).thenReturn("corr-123");

        doThrow(new RuntimeException("Tracker failed")).when(performanceTracker).recordReceive("corr-123");

        assertThrows(RuntimeException.class, () -> listener.receiveMessage(jmsMessage));
    }
}
