package com.example.perftester.kubernetes;

import io.fabric8.kubernetes.api.model.Node;
import io.fabric8.kubernetes.api.model.NodeCondition;
import io.fabric8.kubernetes.api.model.NodeStatus;
import io.fabric8.kubernetes.api.model.Quantity;
import io.fabric8.kubernetes.client.KubernetesClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class KubernetesService {

    private final KubernetesClient client;

    public List<KubernetesNodeInfo> getNodeInfo() {
        List<KubernetesNodeInfo> nodeInfoList = new ArrayList<>();

        try {
            var nodes = client.nodes().list().getItems();

            for (Node node : nodes) {
                var nodeInfo = extractNodeInfo(node);
                nodeInfoList.add(nodeInfo);
            }

            log.info("Retrieved information for {} Kubernetes nodes", nodeInfoList.size());
        } catch (Exception e) {
            log.warn("Failed to retrieve Kubernetes node info: {}. "
                    + "This is expected when not running in a Kubernetes cluster.", e.getMessage());
            nodeInfoList.add(KubernetesNodeInfo.unavailable());
        }

        return nodeInfoList;
    }

    private KubernetesNodeInfo extractNodeInfo(Node node) {
        var metadata = node.getMetadata();
        var status = node.getStatus();
        var info = status != null ? status.getNodeInfo() : null;
        var capacity = status != null ? status.getCapacity() : Map.<String, Quantity>of();
        var allocatable = status != null ? status.getAllocatable() : Map.<String, Quantity>of();

        return new KubernetesNodeInfo(
                metadata != null ? metadata.getName() : "unknown",
                info != null ? info.getKubeletVersion() : "unknown",
                info != null ? info.getOsImage() : "unknown",
                info != null ? info.getArchitecture() : "unknown",
                info != null ? info.getContainerRuntimeVersion() : "unknown",
                getQuantityString(capacity, "cpu"),
                getQuantityString(allocatable, "cpu"),
                getQuantityString(capacity, "memory"),
                getQuantityString(allocatable, "memory"),
                getQuantityString(capacity, "ephemeral-storage"),
                getQuantityString(allocatable, "ephemeral-storage"),
                getQuantityString(capacity, "pods"),
                extractConditions(status)
        );
    }

    private String getQuantityString(Map<String, Quantity> quantities, String key) {
        if (quantities == null) {
            return "N/A";
        }
        var quantity = quantities.get(key);
        return quantity != null ? quantity.toString() : "N/A";
    }

    private List<String> extractConditions(NodeStatus status) {
        if (status == null || status.getConditions() == null) {
            return List.of();
        }

        List<String> conditions = new ArrayList<>();
        for (NodeCondition condition : status.getConditions()) {
            var conditionStr = String.format("%s=%s", condition.getType(), condition.getStatus());
            conditions.add(conditionStr);
        }
        return conditions;
    }
}
