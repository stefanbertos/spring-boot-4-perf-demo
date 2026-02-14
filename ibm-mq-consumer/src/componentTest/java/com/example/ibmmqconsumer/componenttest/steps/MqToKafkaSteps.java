package com.example.ibmmqconsumer.componenttest.steps;

import com.example.avro.MqMessage;
import com.example.avro.serialization.AvroDeserializer;
import com.ibm.mq.jakarta.jms.MQQueue;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import jakarta.jms.JMSException;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.kafka.core.KafkaAdmin;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

public class MqToKafkaSteps {

    @Autowired
    private JmsTemplate jmsTemplate;

    @Autowired
    private KafkaAdmin kafkaAdmin;

    private KafkaConsumer<String, MqMessage> consumer;
    private final List<ConsumerRecord<String, MqMessage>> receivedMessages = new ArrayList<>();

    @Before("@mq-to-kafka")
    public void setupConsumer() {
        receivedMessages.clear();
        var props = Map.<String, Object>of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaAdmin.getConfigurationProperties().get("bootstrap.servers"),
                ConsumerConfig.GROUP_ID_CONFIG, "component-test-" + UUID.randomUUID(),
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest",
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class,
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, AvroDeserializer.class
        );
        consumer = new KafkaConsumer<>(props);
        consumer.subscribe(List.of("mq-requests"));
    }

    @When("a JMS message {string} is sent to queue {string} with replyTo {string}")
    public void aJmsMessageIsSentWithReplyTo(String content, String queueName, String replyToQueue) throws JMSException {
        var replyTo = new MQQueue(replyToQueue);
        jmsTemplate.convertAndSend(queueName, content, m -> {
            m.setJMSReplyTo(replyTo);
            m.setJMSCorrelationID(UUID.randomUUID().toString());
            return m;
        });
    }

    @When("a JMS message {string} is sent to queue {string} without replyTo")
    public void aJmsMessageIsSentWithoutReplyTo(String content, String queueName) {
        jmsTemplate.convertAndSend(queueName, content);
    }

    @Then("within {int} seconds a Kafka message appears on {string} with content {string}")
    public void aKafkaMessageAppearsWithContent(int timeoutSeconds, String topic, String expectedContent) {
        await().atMost(Duration.ofSeconds(timeoutSeconds))
                .pollInterval(Duration.ofMillis(500))
                .untilAsserted(() -> {
                    var records = consumer.poll(Duration.ofMillis(200));
                    records.forEach(receivedMessages::add);
                    assertThat(receivedMessages)
                            .extracting(r -> r.value().getContent().toString())
                            .contains(expectedContent);
                });
    }

    @And("the Kafka message has header {string} containing {string}")
    public void theKafkaMessageHasHeaderContaining(String headerName, String expectedSubstring) {
        var found = receivedMessages.stream()
                .anyMatch(r -> {
                    var header = r.headers().lastHeader(headerName);
                    return header != null
                            && new String(header.value(), StandardCharsets.UTF_8).contains(expectedSubstring);
                });
        assertThat(found)
                .as("Expected Kafka header '%s' containing '%s'", headerName, expectedSubstring)
                .isTrue();
    }

    @Then("no Kafka message appears on {string} within {int} seconds")
    public void noKafkaMessageAppears(String topic, int timeoutSeconds) throws InterruptedException {
        if (consumer == null) {
            var props = Map.<String, Object>of(
                    ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaAdmin.getConfigurationProperties().get("bootstrap.servers"),
                    ConsumerConfig.GROUP_ID_CONFIG, "component-test-" + UUID.randomUUID(),
                    ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest",
                    ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class,
                    ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, AvroDeserializer.class
            );
            consumer = new KafkaConsumer<>(props);
            consumer.subscribe(List.of(topic));
            consumer.poll(Duration.ofMillis(100)); // trigger assignment
        }
        Thread.sleep(timeoutSeconds * 1000L);
        var records = consumer.poll(Duration.ofMillis(500));
        assertThat(records.count()).isZero();
    }

    @After("@mq-to-kafka")
    public void cleanup() {
        if (consumer != null) {
            consumer.close();
            consumer = null;
        }
        receivedMessages.clear();
    }
}
