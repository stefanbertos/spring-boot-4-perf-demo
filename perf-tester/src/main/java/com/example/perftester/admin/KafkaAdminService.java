package com.example.perftester.admin;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import org.apache.kafka.clients.admin.ListOffsetsResult;
import org.apache.kafka.clients.admin.OffsetSpec;
import org.apache.kafka.common.errors.InvalidPartitionsException;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.ListTopicsOptions;
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
        try {
            var newPartitions = Map.of(topicName, NewPartitions.increaseTo(partitions));
            adminClient.createPartitions(newPartitions).all()
                    .get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            log.info("Resized topic '{}' to {} partitions", topicName, partitions);
        } catch (ExecutionException e) {
            if (e.getCause() instanceof InvalidPartitionsException cause) {
                throw new IllegalArgumentException(cause.getMessage(), e);
            }
            throw e;
        }
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
