package com.example.perftester.messaging;

import com.example.perftester.perf.PerformanceTracker;
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
    private final PerformanceTracker performanceTracker;

    public MessageSender(JmsTemplate jmsTemplate,
                         @Value("${app.mq.queue.outbound}") String outboundQueue,
                         PerformanceTracker performanceTracker) {
        this.jmsTemplate = jmsTemplate;
        this.outboundQueue = outboundQueue;
        this.performanceTracker = performanceTracker;
    }

    public void sendMessage(String payload) {
        String messageId = UUID.randomUUID().toString();
        String message = PerformanceTracker.createMessage(messageId, payload);

        performanceTracker.recordSend(messageId);
        jmsTemplate.convertAndSend(outboundQueue, message);

        log.debug("Sent message [{}] to {}", messageId, outboundQueue);
    }
}
