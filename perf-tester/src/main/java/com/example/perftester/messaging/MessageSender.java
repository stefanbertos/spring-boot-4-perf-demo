package com.example.perftester.messaging;

import com.example.perftester.config.AsyncConfig;
import com.example.perftester.perf.PerformanceTracker;
import com.ibm.mq.jakarta.jms.MQQueue;

import jakarta.jms.JMSException;
import jakarta.jms.Queue;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Map;
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
                         MqProperties mqProperties,
                         PerformanceTracker performanceTracker) throws JMSException {
        this.jmsTemplate = jmsTemplate;
        this.outboundQueue = convertToNonJmsQueueNameFormat(mqProperties.queue().outbound());
        this.replyToQueue = createQueue(mqProperties.queue().inbound());
        this.performanceTracker = performanceTracker;
    }

    private MQQueue createQueue(String queueName) throws JMSException {
        return new MQQueue(queueName);
    }

    @Async(AsyncConfig.MQ_SENDER_EXECUTOR)
    public CompletableFuture<Void> sendMessage(String payload) {
        var messageId = UUID.randomUUID().toString();
        var message = PerformanceTracker.createMessage(messageId, payload);

        performanceTracker.recordSend(messageId);
        jmsTemplate.convertAndSend(outboundQueue, message, m -> {
            m.setJMSReplyTo(replyToQueue);
            m.setJMSCorrelationID(messageId);
            return m;
        });

        log.debug("Sent message [{}] to {} with replyTo {}: {}",
                message, outboundQueue, replyToQueue, payload);
        return CompletableFuture.completedFuture(null);
    }

    @Async(AsyncConfig.MQ_SENDER_EXECUTOR)
    public CompletableFuture<Void> sendMessage(String content, Map<String, String> jmsProperties,
                                               String transactionId) {
        var messageId = UUID.randomUUID().toString();
        var message = PerformanceTracker.createMessage(messageId, content);

        performanceTracker.recordSend(messageId);
        jmsTemplate.convertAndSend(outboundQueue, message, m -> {
            m.setJMSReplyTo(replyToQueue);
            m.setJMSCorrelationID(messageId);
            for (var entry : jmsProperties.entrySet()) {
                m.setStringProperty(entry.getKey(), entry.getValue());
            }
            return m;
        });

        log.debug("Sent message [{}] transactionId=[{}] to {} with replyTo {} and {} jms propert(ies): {}",
                messageId, transactionId, outboundQueue, replyToQueue, jmsProperties.size(), content);
        return CompletableFuture.completedFuture(null);
    }

    private String convertToNonJmsQueueNameFormat(String queueName) {
        return String.format("queue:///%s?targetClient=1", queueName);
    }
}
