package com.example.kafkaconsumer.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaRequestListener {

    private final KafkaTemplate<String, String> kafkaTemplate;

    @Value("${app.kafka.topic.response}")
    private String kafkaResponseTopic;

    @KafkaListener(topics = "${app.kafka.topic.request}", groupId = "${spring.kafka.consumer.group-id}")
    public void onMessage(ConsumerRecord<String, String> record) {
        String body = record.value();
        String replyTo = getHeader(record, "mq-reply-to");

        log.debug("Received Kafka request: {}", body);
        String processedMessage = body + " processed";

        log.debug("Publishing response to Kafka topic {}: {}", kafkaResponseTopic, processedMessage);

        MessageBuilder<String> builder = MessageBuilder
                .withPayload(processedMessage)
                .setHeader(KafkaHeaders.TOPIC, kafkaResponseTopic);

        if (replyTo != null) {
            builder.setHeader("mq-reply-to", replyTo);
        }

        Message<String> responseMessage = builder.build();
        kafkaTemplate.send(responseMessage);
    }

    private String getHeader(ConsumerRecord<String, String> record, String headerName) {
        Header header = record.headers().lastHeader(headerName);
        return header != null ? new String(header.value()) : null;
    }
}
