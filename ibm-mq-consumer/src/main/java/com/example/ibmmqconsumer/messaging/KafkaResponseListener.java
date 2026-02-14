package com.example.ibmmqconsumer.messaging;

import com.example.avro.MqMessage;
import com.example.avro.util.KafkaHeaderUtils;
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

    public KafkaResponseListener(JmsTemplate jmsTemplate,
                                 MeterRegistry meterRegistry) {
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
            percentiles = {0.25, 0.5, 0.75, 0.9, 0.95, 0.99})
    @KafkaListener(topics = "${app.kafka.topic.response}", groupId = "${spring.kafka.consumer.group-id}", concurrency = "${app.kafka.consumer.concurrency:20}")
    public void onMessage(ConsumerRecord<String, MqMessage> record) {
        messagesReceived.increment();

        var message = record.value().getContent();
        var replyTo = KafkaHeaderUtils.getHeader(record, "mq-reply-to");
        var correlationId = KafkaHeaderUtils.getHeader(record, "correlationId");
        log.debug("Received Kafka response: {}, replyTo: {}, correlationId=[{}]",
                message, replyTo, correlationId);

        if (replyTo != null) {
            var queueName = extractQueueName(replyTo);

            log.debug("Sending to MQ queue {}: {}", queueName, message);
            jmsTemplate.convertAndSend(queueName, message, m -> {
                if (correlationId != null) {
                    m.setJMSCorrelationID(correlationId);
                }
                return m;
            });
            messagesSentToMq.increment();
        } else {
            log.warn("No mq-reply-to header, dropping message: {}", message);
            messagesDropped.increment();
        }
    }

    private String extractQueueName(String replyTo) {
        // replyTo format is like "queue:///DEV.QUEUE.1" or just "DEV.QUEUE.1"
        if (replyTo.contains("///")) {
            return replyTo.substring(replyTo.lastIndexOf("///") + 3);
        }
        return replyTo;
    }
}
