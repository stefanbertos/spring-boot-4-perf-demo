package com.example.perftester.componenttest.steps;

import io.cucumber.java.After;
import io.cucumber.java.Before;
import jakarta.jms.JMSException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jms.core.JmsTemplate;

import java.util.UUID;

public class BridgeSteps {

    @Autowired
    private JmsTemplate jmsTemplate;

    @Value("${app.mq.queue.inbound}")
    private String inboundQueue;

    @Value("${app.mq.queue.outbound}")
    private String outboundQueue;

    private Thread bridgeThread;

    @Before("@bridge")
    public void startBridge() {
        jmsTemplate.setReceiveTimeout(200L);
        bridgeThread = Thread.ofVirtual()
                .name("ct-mq-bridge-" + UUID.randomUUID())
                .start(() -> {
                    while (!Thread.currentThread().isInterrupted()) {
                        try {
                            var msg = jmsTemplate.receive(outboundQueue);
                            if (msg != null) {
                                var correlationId = msg.getJMSCorrelationID();
                                jmsTemplate.send(inboundQueue, session -> {
                                    var response = session.createTextMessage("response");
                                    response.setJMSCorrelationID(correlationId);
                                    return response;
                                });
                            }
                        } catch (JMSException e) {
                            Thread.currentThread().interrupt();
                        }
                    }
                });
    }

    @After("@bridge")
    public void stopBridge() {
        if (bridgeThread != null) {
            bridgeThread.interrupt();
            bridgeThread = null;
        }
    }
}
