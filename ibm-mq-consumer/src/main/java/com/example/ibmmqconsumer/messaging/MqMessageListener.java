package com.example.ibmmqconsumer.messaging;

import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.Tracer;

import com.example.avro.MqMessage;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.TextMessage;
import java.time.Instant;
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

    private final KafkaTemplate<String, MqMessage> kafkaTemplate;
    private final Counter messagesReceived;
    private final Counter messagesForwarded;
    private final Counter messagesDropped;
    private final Tracer tracer;
    private final String kafkaRequestTopic;

    public MqMessageListener(KafkaTemplate<String, MqMessage> kafkaTemplate,
                            MeterRegistry meterRegistry,
                            Tracer tracer,
                            @Value("${app.kafka.topic.request}") String kafkaRequestTopic) {
        this.kafkaTemplate = kafkaTemplate;
        this.tracer = tracer;
        this.kafkaRequestTopic = kafkaRequestTopic;
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

        var body = ((TextMessage) message).getText();
        var replyTo = message.getJMSReplyTo();
        var correlationId = message.getJMSCorrelationID();
        var traceId = message.getStringProperty("traceId");

        var span = tracer.spanBuilder("mq-receive-forward-kafka")
                .setSpanKind(SpanKind.CONSUMER)
                .setAttribute("messaging.system", "ibm-mq")
                .setAttribute("messaging.destination", kafkaRequestTopic)
                .setAttribute("messaging.correlation_id", correlationId != null ? correlationId : "")
                .setAttribute("traceId.parent", traceId != null ? traceId : "")
                .startSpan();

        try (var _ = span.makeCurrent()) {
            log.debug("Received MQ message: {} traceId=[{}] correlationId=[{}]", body, traceId, correlationId);

            if (replyTo != null) {
                var replyToString = replyTo.toString();
                var newTraceId = span.getSpanContext().getTraceId();
                var newSpanId = span.getSpanContext().getSpanId();

                log.debug("Publishing to Kafka topic {}, replyTo: {}, traceId: {}",
                        kafkaRequestTopic, replyToString, newTraceId);

                var mqMessage = MqMessage.newBuilder()
                        .setContent(body)
                        .setTimestamp(Instant.now())
                        .build();

                var kafkaMessage = MessageBuilder
                        .withPayload(mqMessage)
                        .setHeader(KafkaHeaders.TOPIC, kafkaRequestTopic)
                        .setHeader("mq-reply-to", replyToString)
                        .setHeader("traceId", newTraceId)
                        .setHeader("spanId", newSpanId)
                        .setHeader("correlationId", correlationId)
                        .setHeader("parentTraceId", traceId)
                        .build();

                kafkaTemplate.send(kafkaMessage);
                messagesForwarded.increment();
            } else {
                log.warn("No replyTo destination set, dropping message: {}", body);
                messagesDropped.increment();
            }
        } catch (Exception e) {
            span.recordException(e);
            throw e;
        } finally {
            span.end();
        }
    }
}
