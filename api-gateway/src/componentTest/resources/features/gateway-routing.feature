Feature: API Gateway routing
  The api-gateway routes incoming requests to the appropriate
  backend services based on path predicates.

  Scenario: Route request to perf-tester backend
    Given the backend "perf-tester" is stubbed to return 200
    When a GET request is sent to the gateway at "/api/perf/health"
    Then the response status is 200

  Scenario: Route request to config-server backend
    Given the backend "config-server" is stubbed to return 200
    When a GET request is sent to the gateway at "/config/application/default"
    Then the response status is 200

  Scenario: Return backend error through gateway
    Given the backend "perf-tester" is stubbed to return 503
    When a GET request is sent to the gateway at "/api/perf/health"
    Then the response status is 503
