Feature: MQ message send and receive
  The perf-tester sends messages to DEV.QUEUE.2 and listens for
  responses on DEV.QUEUE.1, tracking round-trip latency.

  Scenario: Send message to MQ and verify it arrives
    Given the MQ broker is running
    When a performance test message "test-payload" is sent via the REST API
    Then within 5 seconds a message appears on queue "DEV.QUEUE.2" containing "test-payload"

  Scenario: Receive response and track performance
    Given the MQ broker is running
    When a response message is placed on queue "DEV.QUEUE.1" with a known correlationId
    Then the PerformanceTracker records the round trip latency
