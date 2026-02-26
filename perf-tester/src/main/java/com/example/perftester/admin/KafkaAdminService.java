package com.example.perftester.admin;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import org.apache.kafka.clients.admin.ListOffsetsResult;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.admin.OffsetSpec;
import org.apache.kafka.clients.admin.TopicDescription;
import org.apache.kafka.common.errors.InvalidPartitionsException;

import com.example.perftester.config.KafkaAdminProperties;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.ListTopicsOptions;
import org.apache.kafka.clients.admin.NewPartitions;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class KafkaAdminService implements AutoCloseable {

    private static final int TIMEOUT_SECONDS = 30;

    private final AdminClient adminClient;

    public KafkaAdminService(KafkaAdminProperties kafkaAdminProperties) {
        this.adminClient = AdminClient.create(
                Map.of(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaAdminProperties.bootstrapServers()));
        log.info("Kafka AdminClient initialized with bootstrap servers: {}", kafkaAdminProperties.bootstrapServers());
    }

    public void resizeTopic(String topicName, int partitions)
            throws ExecutionException, InterruptedException, TimeoutException {
        var description = adminClient.describeTopics(List.of(topicName))
                .allTopicNames().get(TIMEOUT_SECONDS, TimeUnit.SECONDS).get(topicName);
        int currentPartitions = description.partitions().size();
        if (partitions == currentPartitions) {
            return;
        }
        if (partitions > currentPartitions) {
            increasePartitions(topicName, partitions);
        } else {
            deleteAndRecreate(topicName, partitions, description);
        }
    }

    private void increasePartitions(String topicName, int partitions)
            throws ExecutionException, InterruptedException, TimeoutException {
        try {
            adminClient.createPartitions(Map.of(topicName, NewPartitions.increaseTo(partitions)))
                    .all().get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            log.info("Resized topic '{}' to {} partitions", topicName, partitions);
        } catch (ExecutionException e) {
            if (e.getCause() instanceof InvalidPartitionsException cause) {
                throw new IllegalArgumentException(cause.getMessage(), e);
            }
            throw e;
        }
    }

    private void deleteAndRecreate(String topicName, int partitions, TopicDescription description)
            throws ExecutionException, InterruptedException, TimeoutException {
        var replicationFactor = (short) description.partitions().
                getFirst().replicas().size();
        adminClient.deleteTopics(List.of(topicName)).all().get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        log.info("Deleted topic '{}' to reduce partitions from {} to {}",
                topicName, description.partitions().size(), partitions);
        adminClient.createTopics(List.of(new NewTopic(topicName, partitions, replicationFactor)))
                .all().get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        log.info("Recreated topic '{}' with {} partitions", topicName, partitions);
    }

    public List<TopicInfo> listTopics()
            throws ExecutionException, InterruptedException, TimeoutException {
        var options = new ListTopicsOptions().listInternal(false);
        var topicNames = adminClient.listTopics(options).names()
                .get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        var filteredNames = topicNames.stream()
                .filter(name -> !name.startsWith("_"))
                .sorted()
                .toList();

        if (filteredNames.isEmpty()) {
            return List.of();
        }

        var descriptions = adminClient.describeTopics(filteredNames)
                .allTopicNames().get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        return filteredNames.stream()
                .map(name -> new TopicInfo(name, descriptions.get(name).partitions().size()))
                .toList();
    }

    public TopicInfo getTopicInfo(String topicName)
            throws ExecutionException, InterruptedException, TimeoutException {
        var descriptions = adminClient.describeTopics(List.of(topicName))
                .allTopicNames().get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        var description = descriptions.get(topicName);
        return new TopicInfo(topicName, description.partitions().size());
    }

    public long getTotalConsumerGroupLag(String groupId)
            throws ExecutionException, InterruptedException, TimeoutException {
        var offsets = adminClient.listConsumerGroupOffsets(groupId)
                .partitionsToOffsetAndMetadata().get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        if (offsets.isEmpty()) {
            return 0;
        }
        var topicPartitions = offsets.keySet().stream()
                .collect(Collectors.toMap(tp -> tp, tp -> OffsetSpec.latest()));
        Map<org.apache.kafka.common.TopicPartition, ListOffsetsResult.ListOffsetsResultInfo> endOffsets =
                adminClient.listOffsets(topicPartitions).all().get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        return offsets.keySet().stream()
                .mapToLong(tp -> {
                    var end = endOffsets.get(tp);
                    var current = offsets.get(tp);
                    return end != null && current != null
                            ? Math.max(0, end.offset() - current.offset()) : 0;
                })
                .sum();
    }

    @Override
    public void close() {
        adminClient.close();
    }

    public record TopicInfo(String topicName, int partitions) {
    }
}
