package com.example.perftester.componenttest.steps;

import com.example.perftester.perf.PerformanceTracker;
import com.example.perftester.persistence.TestRun;
import com.example.perftester.persistence.TestRunRepository;
import com.example.perftester.persistence.TestScenario;
import com.example.perftester.persistence.TestScenarioRepository;
import com.example.perftester.scheduling.ScheduledScenarioService;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.awaitility.Awaitility;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

public class ScheduledScenarioSteps {

    private static final DateTimeFormatter HH_MM = DateTimeFormatter.ofPattern("HH:mm");

    @Autowired
    private TestScenarioRepository testScenarioRepository;

    @Autowired
    private TestRunRepository testRunRepository;

    @Autowired
    private ScheduledScenarioService scheduledScenarioService;

    @Autowired
    private PerformanceTracker performanceTracker;

    private String scenarioName;
    private Long scenarioId;

    @Before("@scheduled")
    public void setup() {
        scenarioName = "sched-ct-" + UUID.randomUUID();
        performanceTracker.markIdle();
    }

    @After("@scheduled")
    public void cleanup() {
        if (scenarioId != null) {
            testScenarioRepository.deleteById(scenarioId);
        }
        if (performanceTracker.isActive()) {
            performanceTracker.markIdle();
        }
    }

    @Given("a scheduled scenario with count {int} and the current minute as scheduled time")
    public void aScheduledScenarioWithCountAndCurrentMinute(int count) {
        var scenario = new TestScenario();
        scenario.setName(scenarioName);
        scenario.setCount(count);
        scenario.setScheduledEnabled(true);
        scenario.setScheduledTime(LocalTime.now().format(HH_MM));
        scenario.setWarmupCount(0);
        scenarioId = testScenarioRepository.save(scenario).getId();
    }

    @Given("the performance tracker is in RUNNING state")
    public void thePerformanceTrackerIsInRunningState() {
        performanceTracker.tryStart(999, "blocker-" + UUID.randomUUID());
    }

    @When("the scheduled service is triggered")
    public void theScheduledServiceIsTriggered() {
        scheduledScenarioService.runScheduledScenarios();
    }

    @Then("within {int} seconds a TestRun for the scheduled scenario reaches COMPLETED status")
    public void withinSecondsATestRunForTheScheduledScenarioReachesCompletedStatus(int seconds) {
        var expectedTestId = "scheduled-" + scenarioName;
        Awaitility.await()
                .atMost(seconds, TimeUnit.SECONDS)
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {
                    var run = findLatestRunForTestId(expectedTestId);
                    assertThat(run).isNotNull();
                    assertThat(run.getStatus()).isEqualTo("COMPLETED");
                });
    }

    @Then("no TestRun is created for the scheduled scenario within {int} seconds")
    public void noTestRunIsCreatedForTheScheduledScenarioWithin(int seconds) throws InterruptedException {
        var expectedTestId = "scheduled-" + scenarioName;
        Thread.sleep(seconds * 1000L);
        assertThat(findLatestRunForTestId(expectedTestId)).isNull();
    }

    private TestRun findLatestRunForTestId(String testId) {
        return testRunRepository.findAllByOrderByStartedAtDesc().stream()
                .filter(r -> testId.equals(r.getTestId()))
                .findFirst()
                .orElse(null);
    }
}
