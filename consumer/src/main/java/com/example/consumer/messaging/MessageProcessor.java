package com.example.consumer.messaging;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class MessageProcessor {

    private final JmsTemplate jmsTemplate;
    private final String outboundQueue;

    public MessageProcessor(JmsTemplate jmsTemplate, @Value("${app.mq.queue.outbound}") String outboundQueue) {
        this.jmsTemplate = jmsTemplate;
        this.outboundQueue = outboundQueue;
    }

    @JmsListener(destination = "${app.mq.queue.inbound}", concurrency = "10-50")
    public void processMessage(String message) {
        log.debug("Received message: {}", message);
        String processedMessage = message + " processed";
        log.debug("Sending processed message to {}: {}", outboundQueue, processedMessage);
        jmsTemplate.convertAndSend(outboundQueue, processedMessage);
    }
}
