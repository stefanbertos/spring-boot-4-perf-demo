package com.example.perftester.messaging;

import com.example.perftester.perf.PerformanceTracker;
import com.ibm.mq.jakarta.jms.MQQueue;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import jakarta.jms.JMSException;
import jakarta.jms.Queue;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
public class MessageSender {

    private final JmsTemplate jmsTemplate;
    private final String outboundQueue;
    private final Queue replyToQueue;
    private final PerformanceTracker performanceTracker;
    private final Tracer tracer;

    public MessageSender(JmsTemplate jmsTemplate,
                         @Value("${app.mq.queue.outbound}") String outboundQueue,
                         @Value("${app.mq.queue.inbound}") String inboundQueue,
                         PerformanceTracker performanceTracker,
                         Tracer tracer) throws JMSException {
        this.jmsTemplate = jmsTemplate;
        this.outboundQueue = outboundQueue;
        this.replyToQueue = createQueue(inboundQueue);
        this.performanceTracker = performanceTracker;
        this.tracer = tracer;
    }

    private MQQueue createQueue(String queueName) throws JMSException {
        return new MQQueue(queueName);
    }

    public void sendMessage(String payload) {
        var messageId = UUID.randomUUID().toString();
        var message = PerformanceTracker.createMessage(messageId, payload);

        var span = tracer.spanBuilder("mq-send")
                .setSpanKind(SpanKind.PRODUCER)
                .setAttribute("messaging.system", "ibm-mq")
                .setAttribute("messaging.destination", outboundQueue)
                .setAttribute("messaging.message_id", messageId)
                .startSpan();

        try (Scope scope = span.makeCurrent()) {
            var traceId = span.getSpanContext().getTraceId();
            var spanId = span.getSpanContext().getSpanId();

            performanceTracker.recordSend(messageId);
            jmsTemplate.convertAndSend(outboundQueue, message, m -> {
                m.setJMSReplyTo(replyToQueue);
                m.setStringProperty("traceId", traceId);
                m.setStringProperty("spanId", spanId);
                m.setJMSCorrelationID(messageId);
                return m;
            });

            log.debug("Sent message [{}] traceId=[{}] to {} with replyTo {}",
                    messageId, traceId, outboundQueue, replyToQueue);
        } catch (Exception e) {
            span.recordException(e);
            throw e;
        } finally {
            span.end();
        }
    }
}
