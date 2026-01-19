package com.example.consumer.messaging;

import jakarta.jms.Destination;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.TextMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MessageProcessor {

    private final JmsTemplate jmsTemplate;

    @JmsListener(destination = "${app.mq.queue.inbound}", concurrency = "10-50")
    public void processMessage(Message message) throws JMSException {
        String body = ((TextMessage) message).getText();
        Destination replyTo = message.getJMSReplyTo();

        log.debug("Received message: {}", body);
        String processedMessage = body + " processed";

        if (replyTo != null) {
            log.debug("Sending processed message to replyTo {}: {}", replyTo, processedMessage);
            jmsTemplate.convertAndSend(replyTo, processedMessage);
        } else {
            log.warn("No replyTo destination set, dropping message: {}", body);
        }
    }
}
