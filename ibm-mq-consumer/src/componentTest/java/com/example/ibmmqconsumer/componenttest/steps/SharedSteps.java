package com.example.ibmmqconsumer.componenttest.steps;

import io.cucumber.java.en.Given;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.kafka.core.KafkaAdmin;

import java.util.List;

public class SharedSteps {

    private final KafkaAdmin kafkaAdmin;

    public SharedSteps(KafkaAdmin kafkaAdmin) {
        this.kafkaAdmin = kafkaAdmin;
    }

    @Given("the MQ queues and Kafka topic {string} are available")
    public void theMqQueuesAndKafkaTopicAreAvailable(String topic) {
        try (var admin = AdminClient.create(kafkaAdmin.getConfigurationProperties())) {
            admin.createTopics(List.of(new NewTopic(topic, 1, (short) 1)))
                    .all().whenComplete((v, e) -> { }).toCompletionStage().toCompletableFuture().join();
        } catch (Exception e) {
            // Topic may already exist
        }
    }
}
