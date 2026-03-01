package com.example.perftester.persistence;

import java.util.Map;

public record InfraProfileDetail(
        long id, String name,
        Map<String, String> logLevels,
        Map<String, Integer> kafkaTopics,
        Map<String, Integer> kubernetesReplicas,
        Map<String, Integer> ibmMqQueues,
        String createdAt, String updatedAt) {
}
