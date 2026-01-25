package com.example.ibmmqconsumer.messaging;

import jakarta.jms.Destination;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.TextMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MqMessageListener {

    private final KafkaTemplate<String, String> kafkaTemplate;

    @Value("${app.kafka.topic.request}")
    private String kafkaRequestTopic;

    @JmsListener(destination = "${app.mq.queue.inbound}", concurrency = "10-50")
    public void onMessage(Message message) throws JMSException {
        String body = ((TextMessage) message).getText();
        Destination replyTo = message.getJMSReplyTo();

        log.debug("Received MQ message: {}", body);

        if (replyTo != null) {
            String replyToString = replyTo.toString();
            log.debug("Publishing to Kafka topic {}, replyTo: {}", kafkaRequestTopic, replyToString);

            org.springframework.messaging.Message<String> kafkaMessage = MessageBuilder
                    .withPayload(body)
                    .setHeader(KafkaHeaders.TOPIC, kafkaRequestTopic)
                    .setHeader("mq-reply-to", replyToString)
                    .build();

            kafkaTemplate.send(kafkaMessage);
        } else {
            log.warn("No replyTo destination set, dropping message: {}", body);
        }
    }
}