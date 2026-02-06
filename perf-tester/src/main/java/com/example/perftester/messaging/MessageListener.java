package com.example.perftester.messaging;

import com.example.perftester.perf.PerformanceTracker;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.TextMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MessageListener {

    private final PerformanceTracker performanceTracker;
    private final Tracer tracer;

    @JmsListener(destination = "${app.mq.queue.inbound}", concurrency = "10-50")
    public void receiveMessage(Message jmsMessage) throws JMSException {
        var message = ((TextMessage) jmsMessage).getText();
        var traceId = jmsMessage.getStringProperty("traceId");
        var correlationId = jmsMessage.getJMSCorrelationID();

        var span = tracer.spanBuilder("mq-receive-response")
                .setSpanKind(SpanKind.CONSUMER)
                .setAttribute("messaging.system", "ibm-mq")
                .setAttribute("messaging.correlation_id", correlationId != null ? correlationId : "")
                .setAttribute("traceId.parent", traceId != null ? traceId : "")
                .startSpan();

        try (Scope scope = span.makeCurrent()) {
            var messageId = PerformanceTracker.extractMessageId(message);
            if (messageId != null) {
                performanceTracker.recordReceive(messageId);
                log.debug("Received response for message [{}] traceId=[{}] correlationId=[{}]",
                        messageId, traceId, correlationId);
            } else {
                log.warn("Received message without valid ID: {}", message);
            }
        } catch (Exception e) {
            span.recordException(e);
            throw e;
        } finally {
            span.end();
        }
    }
}
