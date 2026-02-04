package com.example.perftester.kubernetes;

import io.fabric8.kubernetes.api.model.Node;
import io.fabric8.kubernetes.api.model.NodeCondition;
import io.fabric8.kubernetes.api.model.NodeList;
import io.fabric8.kubernetes.api.model.NodeStatus;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.Quantity;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.NonNamespaceOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KubernetesServiceTest {

    @Mock
    private KubernetesClient client;

    @Mock
    private NonNamespaceOperation<Node, NodeList, Resource<Node>> nodeOperation;

    @InjectMocks
    private KubernetesService kubernetesService;

    @Test
    void getNodeInfoShouldReturnNodeDetails() {
        var node = createNode("test-node", "v1.28.0", "Ubuntu 22.04", "amd64", "containerd://1.7.0");

        var nodeList = new NodeList();
        nodeList.setItems(List.of(node));
        when(client.nodes()).thenReturn(nodeOperation);
        when(nodeOperation.list()).thenReturn(nodeList);

        var result = kubernetesService.getNodeInfo();

        assertThat(result).hasSize(1);
        var info = result.getFirst();
        assertThat(info.nodeName()).isEqualTo("test-node");
        assertThat(info.kubeletVersion()).isEqualTo("v1.28.0");
        assertThat(info.osImage()).isEqualTo("Ubuntu 22.04");
        assertThat(info.architecture()).isEqualTo("amd64");
        assertThat(info.containerRuntime()).isEqualTo("containerd://1.7.0");
        assertThat(info.cpuCapacity()).isEqualTo("4");
        assertThat(info.memoryCapacity()).isEqualTo("16Gi");
        assertThat(info.conditions()).containsExactly("Ready=True");
    }

    @Test
    void getNodeInfoShouldReturnUnavailableOnException() {
        when(client.nodes()).thenThrow(new RuntimeException("Connection refused"));

        var result = kubernetesService.getNodeInfo();

        assertThat(result).hasSize(1);
        var info = result.getFirst();
        assertThat(info.nodeName()).isEqualTo("N/A");
        assertThat(info.kubeletVersion()).isEqualTo("N/A");
    }

    @Test
    void getNodeInfoShouldHandleNullStatus() {
        var node = new Node();
        var metadata = new ObjectMeta();
        metadata.setName("null-status-node");
        node.setMetadata(metadata);
        node.setStatus(null);

        var nodeList = new NodeList();
        nodeList.setItems(List.of(node));
        when(client.nodes()).thenReturn(nodeOperation);
        when(nodeOperation.list()).thenReturn(nodeList);

        var result = kubernetesService.getNodeInfo();

        assertThat(result).hasSize(1);
        var info = result.getFirst();
        assertThat(info.nodeName()).isEqualTo("null-status-node");
        assertThat(info.kubeletVersion()).isEqualTo("unknown");
        assertThat(info.cpuCapacity()).isEqualTo("N/A");
        assertThat(info.conditions()).isEmpty();
    }

    @Test
    void getNodeInfoShouldHandleNullMetadata() {
        var node = new Node();
        node.setMetadata(null);
        node.setStatus(null);

        var nodeList = new NodeList();
        nodeList.setItems(List.of(node));
        when(client.nodes()).thenReturn(nodeOperation);
        when(nodeOperation.list()).thenReturn(nodeList);

        var result = kubernetesService.getNodeInfo();

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().nodeName()).isEqualTo("unknown");
    }

    @Test
    void getNodeInfoShouldHandleMultipleNodes() {
        var node1 = createNode("node-1", "v1.28.0", "Ubuntu", "amd64", "containerd://1.7.0");
        var node2 = createNode("node-2", "v1.28.0", "Ubuntu", "arm64", "containerd://1.7.0");

        var nodeList = new NodeList();
        nodeList.setItems(List.of(node1, node2));
        when(client.nodes()).thenReturn(nodeOperation);
        when(nodeOperation.list()).thenReturn(nodeList);

        var result = kubernetesService.getNodeInfo();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).nodeName()).isEqualTo("node-1");
        assertThat(result.get(1).nodeName()).isEqualTo("node-2");
    }

    @Test
    void getNodeInfoShouldHandleNullConditions() {
        var node = new Node();
        var metadata = new ObjectMeta();
        metadata.setName("no-conditions");
        node.setMetadata(metadata);
        var status = new NodeStatus();
        status.setConditions(null);
        status.setCapacity(Map.of());
        status.setAllocatable(Map.of());
        node.setStatus(status);

        var nodeList = new NodeList();
        nodeList.setItems(List.of(node));
        when(client.nodes()).thenReturn(nodeOperation);
        when(nodeOperation.list()).thenReturn(nodeList);

        var result = kubernetesService.getNodeInfo();

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().conditions()).isEmpty();
    }

    private Node createNode(String name, String kubeletVersion, String osImage,
                            String arch, String containerRuntime) {
        var node = new Node();

        var metadata = new ObjectMeta();
        metadata.setName(name);
        node.setMetadata(metadata);

        var status = new NodeStatus();
        var nodeInfo = new io.fabric8.kubernetes.api.model.NodeSystemInfo();
        nodeInfo.setKubeletVersion(kubeletVersion);
        nodeInfo.setOsImage(osImage);
        nodeInfo.setArchitecture(arch);
        nodeInfo.setContainerRuntimeVersion(containerRuntime);
        status.setNodeInfo(nodeInfo);

        status.setCapacity(Map.of(
                "cpu", new Quantity("4"),
                "memory", new Quantity("16Gi"),
                "ephemeral-storage", new Quantity("100Gi"),
                "pods", new Quantity("110")
        ));
        status.setAllocatable(Map.of(
                "cpu", new Quantity("3800m"),
                "memory", new Quantity("15Gi"),
                "ephemeral-storage", new Quantity("90Gi"),
                "pods", new Quantity("110")
        ));

        var readyCondition = new NodeCondition();
        readyCondition.setType("Ready");
        readyCondition.setStatus("True");
        status.setConditions(List.of(readyCondition));

        node.setStatus(status);
        return node;
    }
}
