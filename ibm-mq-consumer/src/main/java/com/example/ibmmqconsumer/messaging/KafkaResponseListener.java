package com.example.ibmmqconsumer.messaging;

import com.example.avro.MqMessage;
import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
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
    private final Tracer tracer;

    public KafkaResponseListener(JmsTemplate jmsTemplate,
                                MeterRegistry meterRegistry,
                                Tracer tracer) {
        this.jmsTemplate = jmsTemplate;
        this.tracer = tracer;
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
    public void onMessage(ConsumerRecord<String, MqMessage> record) {
        messagesReceived.increment();

        String message = record.value().getContent();
        String replyTo = getHeader(record, "mq-reply-to");
        String correlationId = getHeader(record, "correlationId");
        String parentTraceId = getHeader(record, "traceId");
        String parentSpanId = getHeader(record, "spanId");

        Span span = tracer.spanBuilder("kafka-receive-forward-mq")
                .setSpanKind(SpanKind.PRODUCER)
                .setAttribute("messaging.system", "ibm-mq")
                .setAttribute("messaging.correlation_id", correlationId != null ? correlationId : "")
                .setAttribute("traceId.parent", parentTraceId != null ? parentTraceId : "")
                .startSpan();

        try (Scope scope = span.makeCurrent()) {
            log.debug("Received Kafka response: {}, replyTo: {}, traceId=[{}], correlationId=[{}]",
                    message, replyTo, parentTraceId, correlationId);

            if (replyTo != null) {
                String queueName = extractQueueName(replyTo);
                String newTraceId = span.getSpanContext().getTraceId();
                String newSpanId = span.getSpanContext().getSpanId();

                span.setAttribute("messaging.destination", queueName);

                log.debug("Sending to MQ queue {}: {} traceId=[{}]", queueName, message, newTraceId);
                jmsTemplate.convertAndSend(queueName, message, m -> {
                    m.setStringProperty("traceId", newTraceId);
                    m.setStringProperty("spanId", newSpanId);
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
        } catch (Exception e) {
            span.recordException(e);
            throw e;
        } finally {
            span.end();
        }
    }

    private String getHeader(ConsumerRecord<String, MqMessage> record, String headerName) {
        var header = record.headers().lastHeader(headerName);
        return header != null ? new String(header.value(), java.nio.charset.StandardCharsets.UTF_8) : null;
    }

    private String extractQueueName(String replyTo) {
        // replyTo format is like "queue:///DEV.QUEUE.1" or just "DEV.QUEUE.1"
        if (replyTo.contains("///")) {
            return replyTo.substring(replyTo.lastIndexOf("///") + 3);
        }
        return replyTo;
    }
}
