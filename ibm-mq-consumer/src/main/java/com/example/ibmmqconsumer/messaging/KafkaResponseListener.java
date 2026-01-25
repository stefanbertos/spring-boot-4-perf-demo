package com.example.ibmmqconsumer.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaResponseListener {

    private final JmsTemplate jmsTemplate;

    @KafkaListener(topics = "${app.kafka.topic.response}", groupId = "${spring.kafka.consumer.group-id}")
    public void onMessage(ConsumerRecord<String, String> record) {
        String message = record.value();
        String replyTo = getHeader(record, "mq-reply-to");

        log.debug("Received Kafka response: {}, replyTo: {}", message, replyTo);

        if (replyTo != null) {
            String queueName = extractQueueName(replyTo);
            log.debug("Sending to MQ queue {}: {}", queueName, message);
            jmsTemplate.convertAndSend(queueName, message);
        } else {
            log.warn("No mq-reply-to header, dropping message: {}", message);
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