package com.example.perftester.componenttest.steps;

import com.example.perftester.messaging.MessageSender;
import com.example.perftester.perf.PerformanceTracker;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.jms.core.JmsTemplate;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

public class MessageSendReceiveSteps {

    private final MessageSender messageSender;
    private final JmsTemplate jmsTemplate;
    private final PerformanceTracker performanceTracker;

    private String trackedMessageId;

    public MessageSendReceiveSteps(MessageSender messageSender, JmsTemplate jmsTemplate,
                                   PerformanceTracker performanceTracker) {
        this.messageSender = messageSender;
        this.jmsTemplate = jmsTemplate;
        this.performanceTracker = performanceTracker;
    }

    @Given("the MQ broker is running")
    public void theMqBrokerIsRunning() {
        // MQ container is started by IbmMqContainerConfig
    }

    @When("a performance test message {string} is sent via the REST API")
    public void aPerformanceTestMessageIsSent(String payload) {
        performanceTracker.startTest(1);
        messageSender.sendMessage(payload);
    }

    @Then("within {int} seconds a message appears on queue {string} containing {string}")
    public void aMessageAppearsOnQueueContaining(int timeoutSeconds, String queueName, String expectedContent) {
        jmsTemplate.setReceiveTimeout(1000);
        await().atMost(Duration.ofSeconds(timeoutSeconds))
                .pollInterval(Duration.ofMillis(500))
                .untilAsserted(() -> {
                    var message = jmsTemplate.receiveAndConvert(queueName);
                    assertThat(message).isNotNull();
                    assertThat(message.toString()).contains(expectedContent);
                });
    }

    @When("a response message is placed on queue {string} with a known correlationId")
    public void aResponseMessageIsPlacedOnQueue(String queueName) {
        trackedMessageId = "test-" + System.nanoTime();
        var message = PerformanceTracker.createMessage(trackedMessageId, "response-payload");

        performanceTracker.startTest(1);
        performanceTracker.recordSend(trackedMessageId);

        jmsTemplate.convertAndSend(queueName, message, m -> {
            m.setJMSCorrelationID(trackedMessageId);
            return m;
        });
    }

    @Then("the PerformanceTracker records the round trip latency")
    public void thePerformanceTrackerRecordsLatency() {
        await().atMost(Duration.ofSeconds(10))
                .pollInterval(Duration.ofMillis(500))
                .untilAsserted(() -> {
                    var result = performanceTracker.getResult();
                    assertThat(result.completedMessages()).isGreaterThan(0);
                });
    }
}
