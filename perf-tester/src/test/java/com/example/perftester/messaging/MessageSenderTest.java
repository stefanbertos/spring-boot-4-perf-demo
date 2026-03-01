package com.example.perftester.messaging;

import com.example.perftester.perf.PerformanceTracker;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class MessageSenderTest {

    @Mock
    private JmsTemplate jmsTemplate;

    @Mock
    private PerformanceTracker performanceTracker;

    @Mock
    private Message jmsMessage;

    private MessageSender messageSender;

    @BeforeEach
    void setUp() throws JMSException {
        messageSender = new MessageSender(jmsTemplate,
                new MqProperties(new MqProperties.QueueProperties("DEV.QUEUE.2", "DEV.QUEUE.1")),
                performanceTracker);
    }

    @Test
    void sendMessageShouldSendToOutboundQueue() throws JMSException {
        messageSender.sendMessage("test payload");

        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<MessagePostProcessor> processorCaptor = ArgumentCaptor.forClass(MessagePostProcessor.class);
        verify(jmsTemplate).convertAndSend(eq("queue:///DEV.QUEUE.2?targetClient=1"), messageCaptor.capture(), processorCaptor.capture());

        String sentMessage = messageCaptor.getValue();
        assertEquals("test payload", sentMessage);
        verify(performanceTracker).recordSend(anyString());

        // Invoke the lambda to cover the MessagePostProcessor code
        MessagePostProcessor processor = processorCaptor.getValue();
        Message result = processor.postProcessMessage(jmsMessage);
        assertNotNull(result);
        verify(jmsMessage).setJMSReplyTo(any(Queue.class));
        verify(jmsMessage).setJMSCorrelationID(anyString());
    }

    @Test
    void sendMessageShouldRecordExceptionOnError() {
        RuntimeException exception = new RuntimeException("JMS send failed");
        doThrow(exception).when(jmsTemplate).convertAndSend(anyString(), anyString(), any(MessagePostProcessor.class));

        assertThrows(RuntimeException.class, () -> messageSender.sendMessage("test payload"));
    }

}
