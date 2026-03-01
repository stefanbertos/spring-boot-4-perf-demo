package com.example.perftester.componenttest.steps;

import com.example.perftester.perf.ThresholdDef;
import com.example.perftester.persistence.TestRunRepository;
import com.example.perftester.persistence.TestScenarioRequest;
import com.example.perftester.persistence.TestScenarioService;
import com.example.perftester.perf.TestStartResponse;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.awaitility.Awaitility;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

public class ThresholdEvaluationSteps {

    @Autowired
    private TestScenarioService testScenarioService;

    @Autowired
    private TestRunRepository testRunRepository;

    @Value("${local.server.port}")
    private int serverPort;

    private final RestTemplate restTemplate = new RestTemplate();

    private String scenarioName;
    private Long scenarioId;
    private Long entityId;

    @Before("@threshold")
    public void setup() {
        scenarioName = "threshold-ct-" + UUID.randomUUID();
    }

    @After("@threshold")
    public void cleanup() {
        if (scenarioId != null) {
            testScenarioService.delete(scenarioId);
        }
    }

    @Given("a scenario with count {int} and an AVG_LATENCY LT {double} threshold")
    public void aScenarioWithCountAndAvgLatencyThreshold(int count, double thresholdValue) {
        var thresholds = List.of(new ThresholdDef("AVG_LATENCY", "LT", thresholdValue));
        var request = new TestScenarioRequest(
                scenarioName, count, null, false, null, 0, null, null, null, thresholds);
        var detail = testScenarioService.create(request);
        scenarioId = detail.id();
    }

    @When("the REST API test is started for that scenario")
    public void theRestApiTestIsStartedForThatScenario() {
        var url = "http://localhost:" + serverPort
                + "/api/perf/send?count=1&timeoutSeconds=10&scenarioId=" + scenarioId;
        var headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_PLAIN);
        var entity = new HttpEntity<>("", headers);
        var response = restTemplate.exchange(url, HttpMethod.POST, entity, TestStartResponse.class);
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        entityId = response.getBody().id();
    }

    @Then("the TestRun threshold status is PASSED")
    public void theTestRunThresholdStatusIsPassed() {
        awaitThresholdStatus("PASSED");
    }

    @Then("the TestRun threshold status is FAILED")
    public void theTestRunThresholdStatusIsFailed() {
        awaitThresholdStatus("FAILED");
    }

    private void awaitThresholdStatus(String expected) {
        Awaitility.await()
                .atMost(15, TimeUnit.SECONDS)
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {
                    var run = testRunRepository.findById(entityId).orElseThrow();
                    assertThat(run.getThresholdStatus()).isEqualTo(expected);
                });
    }
}
