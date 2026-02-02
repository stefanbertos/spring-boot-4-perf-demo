package com.example.perftester.kubernetes;

import java.util.List;

public record KubernetesNodeInfo(
        String nodeName,
        String kubeletVersion,
        String osImage,
        String architecture,
        String containerRuntime,
        String cpuCapacity,
        String cpuAllocatable,
        String memoryCapacity,
        String memoryAllocatable,
        String ephemeralStorageCapacity,
        String ephemeralStorageAllocatable,
        String podsCapacity,
        List<String> conditions
) {
    public static KubernetesNodeInfo unavailable() {
        return new KubernetesNodeInfo(
                "N/A", "N/A", "N/A", "N/A", "N/A",
                "N/A", "N/A", "N/A", "N/A", "N/A", "N/A", "N/A",
                List.of()
        );
    }
}
