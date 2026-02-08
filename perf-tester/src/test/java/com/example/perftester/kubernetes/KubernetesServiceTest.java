package com.example.perftester.kubernetes;

import io.fabric8.kubernetes.api.model.Node;
import io.fabric8.kubernetes.api.model.NodeList;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.NonNamespaceOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KubernetesServiceTest {

    @TempDir
    Path tempDir;

    @Mock
    private KubernetesClient client;

    @Mock
    private NonNamespaceOperation<Node, NodeList, Resource<Node>> nodeOperation;

    @Test
    void exportNodeInfoShouldCreateJsonFile() throws IOException {
        var node = createNode("test-node");

        var nodeList = new NodeList();
        nodeList.setItems(List.of(node));
        when(client.nodes()).thenReturn(nodeOperation);
        when(nodeOperation.list()).thenReturn(nodeList);

        var service = new KubernetesService(client, tempDir.toString());
        var result = service.exportNodeInfo();

        assertThat(result).isNotNull();
        assertThat(Files.exists(Path.of(result))).isTrue();
        assertThat(Files.size(Path.of(result))).isGreaterThan(0);
    }

    @Test
    void exportNodeInfoShouldReturnNullOnException() {
        when(client.nodes()).thenThrow(new RuntimeException("Connection refused"));

        var service = new KubernetesService(client, tempDir.toString());
        var result = service.exportNodeInfo();

        assertThat(result).isNull();
    }

    @Test
    void exportNodeInfoShouldProduceValidJson() throws IOException {
        var node = createNode("my-node");
        var nodeList = new NodeList();
        nodeList.setItems(List.of(node));
        when(client.nodes()).thenReturn(nodeOperation);
        when(nodeOperation.list()).thenReturn(nodeList);

        var service = new KubernetesService(client, tempDir.toString());
        var result = service.exportNodeInfo();

        String content = Files.readString(Path.of(result));
        assertThat(content).contains("my-node");
        assertThat(content).startsWith("{");
    }

    @Test
    void exportNodeInfoShouldHandleMultipleNodes() throws IOException {
        var node1 = createNode("node-1");
        var node2 = createNode("node-2");
        var nodeList = new NodeList();
        nodeList.setItems(List.of(node1, node2));
        when(client.nodes()).thenReturn(nodeOperation);
        when(nodeOperation.list()).thenReturn(nodeList);

        var service = new KubernetesService(client, tempDir.toString());
        var result = service.exportNodeInfo();

        String content = Files.readString(Path.of(result));
        assertThat(content).contains("node-1");
        assertThat(content).contains("node-2");
    }

    private Node createNode(String name) {
        var node = new Node();
        var metadata = new ObjectMeta();
        metadata.setName(name);
        node.setMetadata(metadata);
        return node;
    }
}
