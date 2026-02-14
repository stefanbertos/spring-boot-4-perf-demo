@mq-to-kafka
Feature: MQ to Kafka bridging
  The ibm-mq-consumer listens on DEV.QUEUE.2 and forwards messages to the
  Kafka "mq-requests" topic, preserving the replyTo destination as a Kafka header.
  Messages without a replyTo are dropped.

  Scenario: Forward MQ message to Kafka with replyTo
    Given the MQ queues and Kafka topic "mq-requests" are available
    When a JMS message "hello" is sent to queue "DEV.QUEUE.2" with replyTo "DEV.QUEUE.1"
    Then within 10 seconds a Kafka message appears on "mq-requests" with content "hello"
    And the Kafka message has header "mq-reply-to" containing "DEV.QUEUE.1"

  Scenario: Drop MQ message without replyTo
    Given the MQ queues and Kafka topic "mq-requests" are available
    When a JMS message "no-reply" is sent to queue "DEV.QUEUE.2" without replyTo
    Then no Kafka message appears on "mq-requests" within 5 seconds
