package com.example.perftester.messaging;

import com.example.perftester.perf.PerformanceTracker;
import com.ibm.mq.jakarta.jms.MQQueue;

import jakarta.jms.JMSException;
import jakarta.jms.Queue;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
public class MessageSender {

    private final JmsTemplate jmsTemplate;
    private final String outboundQueue;
    private final Queue replyToQueue;
    private final PerformanceTracker performanceTracker;

    public MessageSender(JmsTemplate jmsTemplate,
                         @Value("${app.mq.queue.outbound}") String outboundQueue,
                         @Value("${app.mq.queue.inbound}") String inboundQueue,
                         PerformanceTracker performanceTracker) throws JMSException {
        this.jmsTemplate = jmsTemplate;
        this.outboundQueue = outboundQueue;
        this.replyToQueue = createQueue(inboundQueue);
        this.performanceTracker = performanceTracker;
    }

    private MQQueue createQueue(String queueName) throws JMSException {
        return new MQQueue(queueName);
    }

    @Async
    public CompletableFuture<Void> sendMessage(String payload) {
        var messageId = UUID.randomUUID().toString();
        var message = PerformanceTracker.createMessage(messageId, payload);

        performanceTracker.recordSend(messageId);
        jmsTemplate.convertAndSend(outboundQueue, message, m -> {
            m.setJMSReplyTo(replyToQueue);
            m.setJMSCorrelationID(messageId);
            return m;
        });

        log.debug("Sent message [{}] to {} with replyTo {}",
                messageId, outboundQueue, replyToQueue);
        return CompletableFuture.completedFuture(null);
    }
}
