Feature: Scheduled scenario execution
  The ScheduledScenarioService triggers matching scenarios at the configured time
  and skips execution when a test is already running.

  @scheduled @bridge
  Scenario: Scheduled scenario triggers when the time matches
    Given a scheduled scenario with count 1 and the current minute as scheduled time
    When the scheduled service is triggered
    Then within 15 seconds a TestRun for the scheduled scenario reaches COMPLETED status

  @scheduled
  Scenario: Scheduler skips when a test is already in RUNNING state
    Given a scheduled scenario with count 1 and the current minute as scheduled time
    And the performance tracker is in RUNNING state
    When the scheduled service is triggered
    Then no TestRun is created for the scheduled scenario within 3 seconds