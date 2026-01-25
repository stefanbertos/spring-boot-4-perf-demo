package com.example.ibmmqconsumer.messaging;

import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class KafkaResponseListener {

    private final JmsTemplate jmsTemplate;
    private final Counter messagesReceived;
    private final Counter messagesSentToMq;
    private final Counter messagesDropped;

    public KafkaResponseListener(JmsTemplate jmsTemplate, MeterRegistry meterRegistry) {
        this.jmsTemplate = jmsTemplate;
        this.messagesReceived = Counter.builder("kafka.response.messages.received")
                .description("Total Kafka response messages received")
                .tag("listener", "kafka-to-mq")
                .register(meterRegistry);
        this.messagesSentToMq = Counter.builder("kafka.response.messages.sent")
                .description("Messages sent to MQ")
                .tag("listener", "kafka-to-mq")
                .register(meterRegistry);
        this.messagesDropped = Counter.builder("kafka.response.messages.dropped")
                .description("Messages dropped (no replyTo header)")
                .tag("listener", "kafka-to-mq")
                .register(meterRegistry);
    }

    @Timed(value = "kafka.response.process.time",
           description = "Time to process Kafka response and send to MQ",
           histogram = true,
           percentiles = {0.95, 0.99})
    @KafkaListener(topics = "${app.kafka.topic.response}", groupId = "${spring.kafka.consumer.group-id}", concurrency = "10")
    public void onMessage(ConsumerRecord<String, String> record) {
        messagesReceived.increment();

        String message = record.value();
        String replyTo = getHeader(record, "mq-reply-to");

        log.debug("Received Kafka response: {}, replyTo: {}", message, replyTo);

        if (replyTo != null) {
            String queueName = extractQueueName(replyTo);
            log.debug("Sending to MQ queue {}: {}", queueName, message);
            jmsTemplate.convertAndSend(queueName, message);
            messagesSentToMq.increment();
        } else {
            log.warn("No mq-reply-to header, dropping message: {}", message);
            messagesDropped.increment();
        }
    }

    private String getHeader(ConsumerRecord<String, String> record, String headerName) {
        var header = record.headers().lastHeader(headerName);
        return header != null ? new String(header.value()) : null;
    }

    private String extractQueueName(String replyTo) {
        // replyTo format is like "queue:///DEV.QUEUE.1" or just "DEV.QUEUE.1"
        if (replyTo.contains("///")) {
            return replyTo.substring(replyTo.lastIndexOf("///") + 3);
        }
        return replyTo;
    }
}
