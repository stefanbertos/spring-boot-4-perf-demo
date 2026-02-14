Feature: Config server serves application configuration
  The config-server uses a native filesystem backend to serve
  configuration properties to client applications.

  Scenario: Serve perf-tester configuration
    Given the config-server is running with native profile
    When configuration for application "perf-tester" is requested
    Then the response contains property "app.mq.queue.inbound" with value "DEV.QUEUE.1"

  Scenario: Serve shared application configuration
    Given the config-server is running with native profile
    When configuration for application "application" is requested
    Then the response contains property "management.endpoints.web.exposure.include" with value "health,info,prometheus"

  Scenario: Serve kafka-consumer configuration
    Given the config-server is running with native profile
    When configuration for application "kafka-consumer" is requested
    Then the response contains property "app.kafka.topic.request" with value "mq-requests"
