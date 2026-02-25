package com.example.kafkaconsumer.messaging;

import com.example.avro.MqMessage;
import com.example.avro.util.KafkaHeaderUtils;
import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;

import java.time.Instant;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class KafkaRequestListener {

    private final KafkaTemplate<String, MqMessage> kafkaTemplate;
    private final Counter messagesReceived;
    private final Counter messagesProcessed;
    private final String kafkaResponseTopic;

    public KafkaRequestListener(KafkaTemplate<String, MqMessage> kafkaTemplate,
                                MeterRegistry meterRegistry,
                                @Value("${app.kafka.topic.response}") String kafkaResponseTopic) {
        this.kafkaTemplate = kafkaTemplate;
        this.kafkaResponseTopic = kafkaResponseTopic;
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
           percentiles = {0.25, 0.5, 0.75, 0.9, 0.95, 0.99})
    @KafkaListener(topics = "${app.kafka.topic.request}", groupId = "${spring.kafka.consumer.group-id}", concurrency = "${app.kafka.consumer.concurrency:20}")
    public void onMessage(ConsumerRecord<String, MqMessage> record) {
        messagesReceived.increment();

        var body = record.value().getContent();
        var replyTo = KafkaHeaderUtils.getHeader(record, "mq-reply-to");
        var correlationId = KafkaHeaderUtils.getHeader(record, "correlationId");

            log.debug("Received Kafka request: {} correlationId=[{}]",
                    body, correlationId);

            var processedContent = body + " processed";

            log.debug("Publishing response to Kafka topic {}: {}",
                    kafkaResponseTopic, processedContent);

            var responsePayload = MqMessage.newBuilder()
                    .setContent(processedContent)
                    .setTimestamp(Instant.now())
                    .build();

            var builder = MessageBuilder
                    .withPayload(responsePayload)
                    .setHeader(KafkaHeaders.TOPIC, kafkaResponseTopic);

            if (replyTo != null) {
                builder.setHeader("mq-reply-to", replyTo);
            }
            if (correlationId != null) {
                builder.setHeader("correlationId", correlationId);
            }

            var responseMessage = builder.build();
            kafkaTemplate.send(responseMessage);
            messagesProcessed.increment();
    }

}
