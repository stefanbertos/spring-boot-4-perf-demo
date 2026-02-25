package com.example.perftester.componenttest.steps;

import com.example.perftester.perf.PerformanceTracker;
import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import jakarta.jms.JMSException;
import jakarta.jms.TextMessage;
import org.awaitility.Awaitility;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.web.client.RestTemplate;
import org.testcontainers.containers.GenericContainer;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

public class MessageSendReceiveSteps {

    @Autowired
    private JmsTemplate jmsTemplate;

    @Autowired
    private PerformanceTracker performanceTracker;

    @Autowired
    private GenericContainer<?> ibmMqContainer;

    @Value("${local.server.port}")
    private int serverPort;

    @Value("${app.mq.queue.inbound}")
    private String inboundQueue;

    private final RestTemplate restTemplate = new RestTemplate();

    private String pendingMessageId;

    @Before
    public void setUp() {
        jmsTemplate.setReceiveTimeout(500L);
    }

    @Given("the MQ broker is running")
    public void theMqBrokerIsRunning() {
        assertThat(ibmMqContainer.isRunning()).isTrue();
    }

    @When("a performance test message {string} is sent via the REST API")
    public void aPerformanceTestMessageIsSentViaTheRestApi(String payload) {
        var headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_PLAIN);
        var entity = new HttpEntity<>(payload, headers);
        var url = "http://localhost:" + serverPort + "/api/perf/send?count=1&timeoutSeconds=2";
        var response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                entity,
                String.class);
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
    }

    @Then("within {int} seconds a message appears on queue {string} containing {string}")
    public void withinSecondsAMessageAppearsOnQueueContaining(int seconds, String queueName, String expected) {
        Awaitility.await()
                .atMost(seconds, TimeUnit.SECONDS)
                .pollInterval(Duration.ofMillis(500))
                .untilAsserted(() -> {
                    var msg = jmsTemplate.receive(queueName);
                    assertThat(msg).isNotNull();
                    assertThat(extractText(msg)).contains(expected);
                });
    }

    @When("a response message is placed on queue {string} with a known correlationId")
    public void aResponseMessageIsPlacedOnQueueWithAKnownCorrelationId(String queueName)
            throws InterruptedException {
        // Give Scenario 1's background thread time to call startTest(), then wait for it to finish
        Thread.sleep(500);
        Awaitility.await()
                .atMost(5, TimeUnit.SECONDS)
                .pollInterval(Duration.ofMillis(200))
                .until(() -> !"RUNNING".equals(performanceTracker.getProgressSnapshot().status()));

        pendingMessageId = UUID.randomUUID().toString();
        performanceTracker.startTest(1, "ct-scenario-2-" + pendingMessageId);
        performanceTracker.recordSend(pendingMessageId);

        var messageText = PerformanceTracker.createMessage(pendingMessageId, "ct-response");
        jmsTemplate.send(queueName, session -> session.createTextMessage(messageText));
    }

    @Then("the PerformanceTracker records the round trip latency")
    public void thePerformanceTrackerRecordsTheRoundTripLatency() throws InterruptedException {
        var completed = performanceTracker.awaitCompletion(5, TimeUnit.SECONDS);
        assertThat(completed).isTrue();
        var result = performanceTracker.getResult();
        assertThat(result.completedMessages()).isEqualTo(1);
        assertThat(result.avgLatencyMs()).isGreaterThanOrEqualTo(0);
    }

    private String extractText(jakarta.jms.Message msg) {
        try {
            return ((TextMessage) msg).getText();
        } catch (JMSException e) {
            throw new RuntimeException("Failed to extract JMS message text", e);
        }
    }
}
