Feature: Threshold evaluation after test completion
  After a performance test finishes, thresholds defined on the scenario are evaluated
  and the result (PASSED or FAILED) is written to the TestRun in the database.

  @threshold @bridge
  Scenario: Threshold status is PASSED when all conditions are met
    Given a scenario with count 1 and an AVG_LATENCY LT 10000 threshold
    When the REST API test is started for that scenario
    Then the TestRun threshold status is PASSED

  @threshold @bridge
  Scenario: Threshold status is FAILED when conditions are not met
    Given a scenario with count 1 and an AVG_LATENCY LT 0.001 threshold
    When the REST API test is started for that scenario
    Then the TestRun threshold status is FAILED
