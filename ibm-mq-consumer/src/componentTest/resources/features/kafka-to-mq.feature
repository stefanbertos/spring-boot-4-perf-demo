@kafka-to-mq
Feature: Kafka to MQ response bridging
  The ibm-mq-consumer listens on the Kafka "mq-responses" topic and forwards
  messages to the MQ queue specified in the "mq-reply-to" header.
  Messages without the header are dropped.

  Scenario: Forward Kafka response to MQ
    Given the MQ queues and Kafka topic "mq-responses" are available
    When a Kafka message with content "result" and header "mq-reply-to" = "queue:///DEV.QUEUE.1" is sent to "mq-responses"
    Then within 10 seconds a JMS message appears on queue "DEV.QUEUE.1" with text "result"

  Scenario: Drop Kafka response without replyTo header
    Given the MQ queues and Kafka topic "mq-responses" are available
    When a Kafka message with content "orphan" without header "mq-reply-to" is sent to "mq-responses"
    Then no JMS message appears on queue "DEV.QUEUE.1" within 5 seconds
