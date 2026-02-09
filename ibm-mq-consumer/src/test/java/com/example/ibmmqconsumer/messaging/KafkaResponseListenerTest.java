package com.example.ibmmqconsumer.messaging;

import com.example.avro.MqMessage;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessagePostProcessor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class KafkaResponseListenerTest {

    @Mock
    private JmsTemplate jmsTemplate;

    @Mock
    private Message jmsMessage;

    private MeterRegistry meterRegistry;
    private KafkaResponseListener listener;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        listener = new KafkaResponseListener(jmsTemplate, meterRegistry);
    }

    @Test
    void onMessageShouldSendToMqWhenReplyToIsSet() throws JMSException {
        ConsumerRecord<String, MqMessage> record = createRecord("test message processed",
                "queue:///DEV.QUEUE.1", "corr-123", "parent-trace", "parent-span");

        listener.onMessage(record);

        ArgumentCaptor<String> queueCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<MessagePostProcessor> processorCaptor = ArgumentCaptor.forClass(MessagePostProcessor.class);
        verify(jmsTemplate).convertAndSend(queueCaptor.capture(), messageCaptor.capture(), processorCaptor.capture());

        assertEquals("DEV.QUEUE.1", queueCaptor.getValue());
        assertEquals("test message processed", messageCaptor.getValue());

        // Invoke the lambda to cover the MessagePostProcessor code
        MessagePostProcessor processor = processorCaptor.getValue();
        processor.postProcessMessage(jmsMessage);
        verify(jmsMessage).setJMSCorrelationID("corr-123");
    }

    @Test
    void onMessageShouldExtractQueueNameWithoutPrefix() throws JMSException {
        ConsumerRecord<String, MqMessage> record = createRecord("test message", "DEV.QUEUE.1", null, null, null);

        listener.onMessage(record);

        ArgumentCaptor<String> queueCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<MessagePostProcessor> processorCaptor = ArgumentCaptor.forClass(MessagePostProcessor.class);
        verify(jmsTemplate).convertAndSend(queueCaptor.capture(), anyString(), processorCaptor.capture());

        assertEquals("DEV.QUEUE.1", queueCaptor.getValue());

        // Invoke the lambda to cover the case when correlationId is null
        MessagePostProcessor processor = processorCaptor.getValue();
        processor.postProcessMessage(jmsMessage);
        verify(jmsMessage, never()).setJMSCorrelationID(anyString());
    }

    @Test
    void onMessageShouldDropMessageWhenNoReplyTo() {
        MqMessage mqMessage = MqMessage.newBuilder()
                .setContent("test message")
                .setTimestamp(Instant.now())
                .build();
        ConsumerRecord<String, MqMessage> record = new ConsumerRecord<>("mq-responses", 0, 0, "key", mqMessage);

        listener.onMessage(record);

        verify(jmsTemplate, never()).convertAndSend(anyString(), anyString(), any(MessagePostProcessor.class));

        double dropped = meterRegistry.counter("kafka.response.messages.dropped", "listener", "kafka-to-mq").count();
        assertTrue(dropped >= 1.0);
    }

    @Test
    void onMessageShouldRecordExceptionOnError() {
        ConsumerRecord<String, MqMessage> record = createRecord("test message", "queue:///DEV.QUEUE.1", null, null, null);

        RuntimeException exception = new RuntimeException("JMS send failed");
        doThrow(exception).when(jmsTemplate).convertAndSend(anyString(), anyString(), any(MessagePostProcessor.class));

        assertThrows(RuntimeException.class, () -> listener.onMessage(record));
    }

    @Test
    void countersShouldIncrement() {
        ConsumerRecord<String, MqMessage> record = createRecord("test message", "queue:///DEV.QUEUE.1", null, null, null);

        listener.onMessage(record);

        double received = meterRegistry.counter("kafka.response.messages.received", "listener", "kafka-to-mq").count();
        double sent = meterRegistry.counter("kafka.response.messages.sent", "listener", "kafka-to-mq").count();

        assertTrue(received >= 1.0);
        assertTrue(sent >= 1.0);
    }

    private ConsumerRecord<String, MqMessage> createRecord(String message, String replyTo, String correlationId,
                                                        String traceId, String spanId) {
        MqMessage mqMessage = MqMessage.newBuilder()
                .setContent(message)
                .setTimestamp(Instant.now())
                .build();
        ConsumerRecord<String, MqMessage> record = new ConsumerRecord<>("mq-responses", 0, 0, "key", mqMessage);
        if (replyTo != null) {
            record.headers().add("mq-reply-to", replyTo.getBytes(StandardCharsets.UTF_8));
        }
        if (correlationId != null) {
            record.headers().add("correlationId", correlationId.getBytes(StandardCharsets.UTF_8));
        }
        return record;
    }
}
