Feature: Kafka message processing
  The kafka-consumer receives Avro messages on the request topic,
  appends " processed" to the content, and publishes to the response topic.

  Scenario: Process a single message
    Given the Kafka topics "mq-requests" and "mq-responses" exist
    When a message with content "hello world" is sent to topic "mq-requests"
    Then within 10 seconds a message appears on topic "mq-responses" with content "hello world processed"

  Scenario: Headers are preserved through processing
    Given the Kafka topics "mq-requests" and "mq-responses" exist
    When a message with content "test" and header "mq-reply-to" = "queue:///DEV.QUEUE.1" is sent to topic "mq-requests"
    Then within 10 seconds a message appears on topic "mq-responses" with header "mq-reply-to" = "queue:///DEV.QUEUE.1"

  Scenario: Multiple messages are processed
    Given the Kafka topics "mq-requests" and "mq-responses" exist
    When 5 messages with content prefix "msg" are sent to topic "mq-requests"
    Then within 30 seconds 5 messages appear on topic "mq-responses" each with " processed" suffix
