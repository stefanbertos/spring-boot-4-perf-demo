package com.example.perftester.messaging;

import com.example.perftester.perf.PerformanceTracker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class MessageListener {

    private final PerformanceTracker performanceTracker;

    public MessageListener(PerformanceTracker performanceTracker) {
        this.performanceTracker = performanceTracker;
    }

    @JmsListener(destination = "${app.mq.queue.inbound}")
    public void receiveMessage(String message) {
        String messageId = PerformanceTracker.extractMessageId(message);
        if (messageId != null) {
            performanceTracker.recordReceive(messageId);
            log.debug("Received response for message [{}]", messageId);
        } else {
            log.warn("Received message without valid ID: {}", message);
        }
    }
}
