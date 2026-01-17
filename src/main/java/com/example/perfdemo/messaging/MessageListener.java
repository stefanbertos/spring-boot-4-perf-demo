package com.example.perfdemo.messaging;

import lombok.extern.slf4j.Slf4j;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class MessageListener {

    @JmsListener(destination = "${app.mq.queue.inbound}")
    public void receiveMessage(String message) {
        log.info("Received message: {}", message);
    }
}
