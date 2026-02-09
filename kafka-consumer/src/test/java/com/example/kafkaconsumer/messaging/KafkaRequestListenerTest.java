package com.example.kafkaconsumer.messaging;

import com.example.avro.MqMessage;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.Message;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class KafkaRequestListenerTest {

    @Mock
    private KafkaTemplate<String, MqMessage> kafkaTemplate;

    private MeterRegistry meterRegistry;
    private KafkaRequestListener listener;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        listener = new KafkaRequestListener(kafkaTemplate, meterRegistry, "mq-responses");
    }

    @Test
    void onMessageShouldProcessAndSendResponse() {
        MqMessage mqMessage = MqMessage.newBuilder()
                .setContent("test message")
                .setTimestamp(Instant.now())
                .build();

        ConsumerRecord<String, MqMessage> record = new ConsumerRecord<>(
                "mq-requests", 0, 0, "key", mqMessage);
        record.headers().add("mq-reply-to", "queue:///DEV.QUEUE.1".getBytes(StandardCharsets.UTF_8));
        record.headers().add("correlationId", "corr-123".getBytes(StandardCharsets.UTF_8));

        listener.onMessage(record);

        ArgumentCaptor<Message<MqMessage>> messageCaptor = ArgumentCaptor.forClass(Message.class);
        verify(kafkaTemplate).send(messageCaptor.capture());

        Message<MqMessage> sentMessage = messageCaptor.getValue();
        assertEquals("test message processed", sentMessage.getPayload().getContent());
        assertNotNull(sentMessage.getPayload().getTimestamp());
        assertEquals("mq-responses", sentMessage.getHeaders().get("kafka_topic"));
        assertEquals("queue:///DEV.QUEUE.1", sentMessage.getHeaders().get("mq-reply-to"));
    }

    @Test
    void onMessageShouldHandleNullHeaders() {
        MqMessage mqMessage = MqMessage.newBuilder()
                .setContent("test message")
                .setTimestamp(Instant.now())
                .build();

        ConsumerRecord<String, MqMessage> record = new ConsumerRecord<>(
                "mq-requests", 0, 0, "key", mqMessage);

        listener.onMessage(record);

        ArgumentCaptor<Message<MqMessage>> messageCaptor = ArgumentCaptor.forClass(Message.class);
        verify(kafkaTemplate).send(messageCaptor.capture());

        Message<MqMessage> sentMessage = messageCaptor.getValue();
        assertEquals("test message processed", sentMessage.getPayload().getContent());
    }

    @Test
    void onMessageShouldRecordExceptionOnError() {
        RuntimeException exception = new RuntimeException("Send failed");
        doThrow(exception).when(kafkaTemplate).send(any(Message.class));

        MqMessage mqMessage = MqMessage.newBuilder()
                .setContent("test message")
                .setTimestamp(Instant.now())
                .build();

        ConsumerRecord<String, MqMessage> record = new ConsumerRecord<>(
                "mq-requests", 0, 0, "key", mqMessage);

        try {
            listener.onMessage(record);
        } catch (RuntimeException e) {
            assertEquals("Send failed", e.getMessage());
        }
    }

    @Test
    void countersShouldIncrement() {
        MqMessage mqMessage = MqMessage.newBuilder()
                .setContent("test message")
                .setTimestamp(Instant.now())
                .build();

        ConsumerRecord<String, MqMessage> record = new ConsumerRecord<>(
                "mq-requests", 0, 0, "key", mqMessage);

        listener.onMessage(record);

        double received = meterRegistry.counter("kafka.request.messages.received", "listener", "kafka-processor").count();
        double processed = meterRegistry.counter("kafka.request.messages.processed", "listener", "kafka-processor").count();

        assertTrue(received >= 1.0);
        assertTrue(processed >= 1.0);
    }
}
