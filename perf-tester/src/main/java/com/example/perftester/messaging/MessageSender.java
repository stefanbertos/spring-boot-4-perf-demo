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

    public void sendMessage(String payload) {
        String messageId = UUID.randomUUID().toString();
        String message = PerformanceTracker.createMessage(messageId, payload);

        performanceTracker.recordSend(messageId);
        jmsTemplate.convertAndSend(outboundQueue, message, m -> {
            m.setJMSReplyTo(replyToQueue);
            return m;
        });

        log.debug("Sent message [{}] to {} with replyTo {}", messageId, outboundQueue, replyToQueue);
    }
}

/*
MessageSender with IBM Mq are there some specific headers I can use with MQ, some message formats etc?
With IBM MQ and JMS, you have several headers and message options available:

Standard JMS Headers

  m.setJMSCorrelationID("correlation-123");  // For request/reply matching
  m.setJMSReplyTo(replyToQueue);             // Already using this
  m.setJMSType("MyMessageType");             // Message type identifier
  m.setJMSPriority(5);                       // 0-9 (default 4)
  m.setJMSDeliveryMode(DeliveryMode.PERSISTENT);  // or NON_PERSISTENT
  m.setJMSExpiration(60000);                 // TTL in milliseconds

IBM MQ-Specific Headers (MQMD)

Using MQMessage or JMS properties that map to MQMD fields:

        m.setStringProperty("JMS_IBM_Format", "MQSTR");      // Message format
  m.setStringProperty("JMS_IBM_Character_Set", "1208"); // UTF-8
  m.setIntProperty("JMS_IBM_Encoding", 273);           // Numeric encoding
  m.setIntProperty("JMS_IBM_MsgType", 8);              // Request message
  m.setStringProperty("JMS_IBM_PutApplName", "MyApp"); // Application name
  m.setIntProperty("JMS_IBM_Report_Exception", 1);     // Exception reporting
  m.setIntProperty("JMS_IBM_Report_Expiration", 1);    // Expiry reporting
  m.setIntProperty("JMS_IBM_Report_COA", 1);           // Confirm on arrival
  m.setIntProperty("JMS_IBM_Report_COD", 1);           // Confirm on delivery

Message Formats
  ┌────────┬───────────────────────────────────────────┐
          │ Format │                Description                │
        ├────────┼───────────────────────────────────────────┤
        │ MQSTR  │ String/text message                       │
        ├────────┼───────────────────────────────────────────┤
        │ MQHRF2 │ Rules and formatting header (used by JMS) │
        ├────────┼───────────────────────────────────────────┤
        │ MQCIH  │ CICS information header                   │
        ├────────┼───────────────────────────────────────────┤
        │ MQIIH  │ IMS information header                    │
        ├────────┼───────────────────────────────────────────┤
        │ MQXMIT │ Transmission queue header                 │
        └────────┴───────────────────────────────────────────┘
Example Enhanced Sender

  jmsTemplate.convertAndSend(outboundQueue, message, m -> {
        m.setJMSReplyTo(replyToQueue);
      m.setJMSCorrelationID(messageId);
      m.setJMSPriority(7);  // Higher priority

// IBM MQ specific
      m.setStringProperty("JMS_IBM_Format", "MQSTR");
      m.setIntProperty("JMS_IBM_Report_COA", 1);  // Confirm on arrival

// Custom application headers
      m.setStringProperty("AppVersion", "1.0");
      m.setLongProperty("Timestamp", System.currentTimeMillis());

        return m;
  });

Message Types

  - TextMessage - String content (current usage)
  - BytesMessage - Binary data
  - MapMessage - Key-value pairs
  - ObjectMessage - Serialized Java objects
  - StreamMessage - Sequential primitive types

The JMS_IBM_* properties give you access to the underlying MQMD (Message Descriptor) fields for fine-grained control over MQ behavior.
*/