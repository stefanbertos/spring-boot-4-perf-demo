package com.example.kafkaconsumer.componenttest.steps;

import com.example.avro.MqMessage;
import com.example.avro.serialization.AvroDeserializer;
import com.example.avro.serialization.AvroSerializer;
import io.cucumber.java.After;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.kafka.core.KafkaAdmin;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

public class KafkaProcessingSteps {

    private final KafkaAdmin kafkaAdmin;

    private KafkaConsumer<String, MqMessage> consumer;

    public KafkaProcessingSteps(KafkaAdmin kafkaAdmin) {
        this.kafkaAdmin = kafkaAdmin;
    }

    @Given("the Kafka topics {string} and {string} exist")
    public void theKafkaTopicsExist(String requestTopic, String responseTopic) {
        try (var admin = AdminClient.create(kafkaAdmin.getConfigurationProperties())) {
            var topics = List.of(
                    new NewTopic(requestTopic, 1, (short) 1),
                    new NewTopic(responseTopic, 1, (short) 1)
            );
            admin.createTopics(topics).all().whenComplete((v, e) -> { }).toCompletionStage().toCompletableFuture().join();
        } catch (Exception e) {
            // Topics may already exist from a previous scenario
        }

        // Create a consumer for the response topic
        if (consumer != null) {
            consumer.close();
        }
        var props = Map.<String, Object>of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaAdmin.getConfigurationProperties().get("bootstrap.servers"),
                ConsumerConfig.GROUP_ID_CONFIG, "component-test-" + UUID.randomUUID(),
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest",
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class,
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, AvroDeserializer.class
        );
        consumer = new KafkaConsumer<>(props);
        consumer.subscribe(List.of(responseTopic));
    }

    @When("a message with content {string} is sent to topic {string}")
    public void aMessageIsSentToTopic(String content, String topic) {
        sendMessage(topic, content, Map.of());
    }

    @When("a message with content {string} and header {string} = {string} is sent to topic {string}")
    public void aMessageWithHeaderIsSentToTopic(String content, String headerName, String headerValue, String topic) {
        sendMessage(topic, content, Map.of(headerName, headerValue));
    }

    @When("{int} messages with content prefix {string} are sent to topic {string}")
    public void multipleMessagesAreSentToTopic(int count, String prefix, String topic) {
        for (var i = 0; i < count; i++) {
            sendMessage(topic, prefix + "-" + i, Map.of());
        }
    }

    @Then("within {int} seconds a message appears on topic {string} with content {string}")
    public void aMessageAppearsWithContent(int timeoutSeconds, String topic, String expectedContent) {
        var messages = pollMessages(timeoutSeconds);
        assertThat(messages)
                .extracting(r -> r.value().getContent().toString())
                .contains(expectedContent);
    }

    @Then("within {int} seconds a message appears on topic {string} with header {string} = {string}")
    public void aMessageAppearsWithHeader(int timeoutSeconds, String topic, String headerName, String expectedValue) {
        var messages = pollMessages(timeoutSeconds);
        var found = messages.stream()
                .anyMatch(r -> {
                    var header = r.headers().lastHeader(headerName);
                    return header != null && new String(header.value(), StandardCharsets.UTF_8).equals(expectedValue);
                });
        assertThat(found).as("Expected header %s=%s in response messages", headerName, expectedValue).isTrue();
    }

    @Then("within {int} seconds {int} messages appear on topic {string} each with {string} suffix")
    public void multipleMessagesAppearWithSuffix(int timeoutSeconds, int expectedCount, String topic, String suffix) {
        var allMessages = new ArrayList<String>();
        await().atMost(Duration.ofSeconds(timeoutSeconds))
                .pollInterval(Duration.ofMillis(500))
                .untilAsserted(() -> {
                    var records = consumer.poll(Duration.ofMillis(200));
                    records.forEach(r -> allMessages.add(r.value().getContent().toString()));
                    assertThat(allMessages).hasSize(expectedCount);
                });
        assertThat(allMessages).allMatch(content -> content.endsWith(suffix));
    }

    @After
    public void cleanup() {
        if (consumer != null) {
            consumer.close();
            consumer = null;
        }
    }

    private void sendMessage(String topic, String content, Map<String, String> headers) {
        var props = Map.<String, Object>of(
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaAdmin.getConfigurationProperties().get("bootstrap.servers"),
                ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class,
                ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, AvroSerializer.class
        );
        try (var producer = new KafkaProducer<String, MqMessage>(props)) {
            var message = MqMessage.newBuilder()
                    .setContent(content)
                    .setTimestamp(Instant.now())
                    .build();
            var record = new ProducerRecord<String, MqMessage>(topic, null, message);
            headers.forEach((k, v) ->
                    record.headers().add(new RecordHeader(k, v.getBytes(StandardCharsets.UTF_8))));
            producer.send(record).get();
        } catch (Exception e) {
            throw new RuntimeException("Failed to send test message", e);
        }
    }

    private List<org.apache.kafka.clients.consumer.ConsumerRecord<String, MqMessage>> pollMessages(int timeoutSeconds) {
        var results = new ArrayList<org.apache.kafka.clients.consumer.ConsumerRecord<String, MqMessage>>();
        await().atMost(Duration.ofSeconds(timeoutSeconds))
                .pollInterval(Duration.ofMillis(500))
                .untilAsserted(() -> {
                    var records = consumer.poll(Duration.ofMillis(200));
                    records.forEach(results::add);
                    assertThat(results).isNotEmpty();
                });
        return results;
    }
}
