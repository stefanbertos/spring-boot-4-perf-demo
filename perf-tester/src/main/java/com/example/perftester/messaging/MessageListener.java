package com.example.perftester.messaging;

import com.example.perftester.perf.PerformanceTracker;

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

    @JmsListener(destination = "${app.mq.queue.inbound}", concurrency = "10-50")
    public void receiveMessage(Message jmsMessage) throws JMSException {
        var message = ((TextMessage) jmsMessage).getText();
        var correlationId = jmsMessage.getJMSCorrelationID();

            var messageId = PerformanceTracker.extractMessageId(message);
            if (messageId != null) {
                performanceTracker.recordReceive(messageId);
                log.debug("Received response for message [{}] correlationId=[{}]",
                        messageId, correlationId);
            } else {
                log.warn("Received message without valid ID: {}", message);
            }
    }
}
