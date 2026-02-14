package com.example.ibmmqconsumer.componenttest.steps;

import com.example.avro.MqMessage;
import com.example.avro.serialization.AvroSerializer;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.kafka.core.KafkaAdmin;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

public class KafkaToMqSteps {

    @Autowired
    private JmsTemplate jmsTemplate;

    @Autowired
    private KafkaAdmin kafkaAdmin;

    @When("a Kafka message with content {string} and header {string} = {string} is sent to {string}")
    public void aKafkaMessageWithHeaderIsSentTo(String content, String headerName, String headerValue, String topic) {
        sendKafkaMessage(topic, content, Map.of(headerName, headerValue));
    }

    @When("a Kafka message with content {string} without header {string} is sent to {string}")
    public void aKafkaMessageWithoutHeaderIsSentTo(String content, String headerName, String topic) {
        sendKafkaMessage(topic, content, Map.of());
    }

    @Then("within {int} seconds a JMS message appears on queue {string} with text {string}")
    public void aJmsMessageAppearsOnQueue(int timeoutSeconds, String queueName, String expectedText) {
        jmsTemplate.setReceiveTimeout(1000);
        await().atMost(Duration.ofSeconds(timeoutSeconds))
                .pollInterval(Duration.ofMillis(500))
                .untilAsserted(() -> {
                    var message = jmsTemplate.receiveAndConvert(queueName);
                    assertThat(message).isNotNull();
                    assertThat(message.toString()).isEqualTo(expectedText);
                });
    }

    @Then("no JMS message appears on queue {string} within {int} seconds")
    public void noJmsMessageAppearsOnQueue(String queueName, int timeoutSeconds) throws InterruptedException {
        Thread.sleep(timeoutSeconds * 1000L);
        jmsTemplate.setReceiveTimeout(500);
        var message = jmsTemplate.receiveAndConvert(queueName);
        assertThat(message).isNull();
    }

    private void sendKafkaMessage(String topic, String content, Map<String, String> headers) {
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
            throw new RuntimeException("Failed to send Kafka message", e);
        }
    }
}
