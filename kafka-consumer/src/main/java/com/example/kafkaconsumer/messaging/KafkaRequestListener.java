package com.example.kafkaconsumer.messaging;

import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
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
public class KafkaRequestListener {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final Counter messagesReceived;
    private final Counter messagesProcessed;

    @Value("${app.kafka.topic.response}")
    private String kafkaResponseTopic;

    public KafkaRequestListener(KafkaTemplate<String, String> kafkaTemplate, MeterRegistry meterRegistry) {
        this.kafkaTemplate = kafkaTemplate;
        this.messagesReceived = Counter.builder("kafka.request.messages.received")
                .description("Total Kafka request messages received")
                .tag("listener", "kafka-processor")
                .register(meterRegistry);
        this.messagesProcessed = Counter.builder("kafka.request.messages.processed")
                .description("Messages processed and response sent")
                .tag("listener", "kafka-processor")
                .register(meterRegistry);
    }

    @Timed(value = "kafka.request.process.time",
           description = "Time to process Kafka request and send response",
           histogram = true,
           percentiles = {0.5, 0.75, 0.9, 0.95, 0.99})
    @KafkaListener(topics = "${app.kafka.topic.request}", groupId = "${spring.kafka.consumer.group-id}", concurrency = "10")
    public void onMessage(ConsumerRecord<String, String> record) {
        messagesReceived.increment();

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
        messagesProcessed.increment();
    }

    private String getHeader(ConsumerRecord<String, String> record, String headerName) {
        Header header = record.headers().lastHeader(headerName);
        return header != null ? new String(header.value()) : null;
    }
}
