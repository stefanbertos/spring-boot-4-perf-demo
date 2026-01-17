package com.example.perftester.messaging;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class MessageSender {

    private final JmsTemplate jmsTemplate;
    private final String outboundQueue;

    public MessageSender(JmsTemplate jmsTemplate, @Value("${app.mq.queue.outbound}") String outboundQueue) {
        this.jmsTemplate = jmsTemplate;
        this.outboundQueue = outboundQueue;
    }

    public void sendMessage(String message) {
        log.info("Sending message to {}: {}", outboundQueue, message);
        jmsTemplate.convertAndSend(outboundQueue, message);
    }
}
