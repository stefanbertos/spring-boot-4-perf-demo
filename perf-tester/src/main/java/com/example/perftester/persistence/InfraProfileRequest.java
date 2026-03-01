package com.example.perftester.persistence;

import java.util.Map;

public record InfraProfileRequest(
        String name,
        Map<String, String> logLevels,
        Map<String, Integer> kafkaTopics,
        Map<String, Integer> kubernetesReplicas,
        Map<String, Integer> ibmMqQueues) {
}
