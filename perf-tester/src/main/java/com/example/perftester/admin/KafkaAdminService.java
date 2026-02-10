package com.example.perftester.admin;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewPartitions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class KafkaAdminService implements AutoCloseable {

    private static final int TIMEOUT_SECONDS = 30;

    private final AdminClient adminClient;

    public KafkaAdminService(
            @Value("${app.kafka.bootstrap-servers:localhost:9092}") String bootstrapServers) {
        this.adminClient = AdminClient.create(
                Map.of(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers));
        log.info("Kafka AdminClient initialized with bootstrap servers: {}", bootstrapServers);
    }

    public void resizeTopic(String topicName, int partitions)
            throws ExecutionException, InterruptedException, TimeoutException {
        var newPartitions = Map.of(topicName, NewPartitions.increaseTo(partitions));
        adminClient.createPartitions(newPartitions).all()
                .get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        log.info("Resized topic '{}' to {} partitions", topicName, partitions);
    }

    public TopicInfo getTopicInfo(String topicName)
            throws ExecutionException, InterruptedException, TimeoutException {
        var descriptions = adminClient.describeTopics(List.of(topicName))
                .allTopicNames().get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        var description = descriptions.get(topicName);
        return new TopicInfo(topicName, description.partitions().size());
    }

    @Override
    public void close() {
        adminClient.close();
    }

    public record TopicInfo(String topicName, int partitions) {
    }
}
