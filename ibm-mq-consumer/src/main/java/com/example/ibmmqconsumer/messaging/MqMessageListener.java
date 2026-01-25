package com.example.ibmmqconsumer.messaging;

import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.jms.Destination;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.TextMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class MqMessageListener {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final Counter messagesReceived;
    private final Counter messagesForwarded;
    private final Counter messagesDropped;

    @Value("${app.kafka.topic.request}")
    private String kafkaRequestTopic;

    public MqMessageListener(KafkaTemplate<String, String> kafkaTemplate, MeterRegistry meterRegistry) {
        this.kafkaTemplate = kafkaTemplate;
        this.messagesReceived = Counter.builder("mq.listener.messages.received")
                .description("Total MQ messages received")
                .tag("listener", "mq-to-kafka")
                .register(meterRegistry);
        this.messagesForwarded = Counter.builder("mq.listener.messages.forwarded")
                .description("Messages forwarded to Kafka")
                .tag("listener", "mq-to-kafka")
                .register(meterRegistry);
        this.messagesDropped = Counter.builder("mq.listener.messages.dropped")
                .description("Messages dropped (no replyTo)")
                .tag("listener", "mq-to-kafka")
                .register(meterRegistry);
    }

    @Timed(value = "mq.listener.process.time",
           description = "Time to process MQ message and forward to Kafka",
           histogram = true,
           percentiles = {0.95, 0.99})
    @JmsListener(destination = "${app.mq.queue.inbound}", concurrency = "10-50")
    public void onMessage(Message message) throws JMSException {
        messagesReceived.increment();

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
            messagesForwarded.increment();
        } else {
            log.warn("No replyTo destination set, dropping message: {}", body);
            messagesDropped.increment();
        }
    }
}
