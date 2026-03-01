Feature: Warmup phase followed by main performance test
  When a scenario has warmupCount > 0, warmup messages are sent and completed
  before the main test begins. Only main test messages count towards the result.

  @warmup @bridge
  Scenario: Warmup messages complete before the main test messages are counted
    Given a scenario with warmupCount 1 and count 1
    When the REST API test is started with that warmup scenario
    Then the test completes with 1 completed message
