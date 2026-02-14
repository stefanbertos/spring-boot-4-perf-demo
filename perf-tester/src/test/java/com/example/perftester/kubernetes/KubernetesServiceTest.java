package com.example.perftester.kubernetes;

import io.fabric8.kubernetes.api.model.Node;
import io.fabric8.kubernetes.api.model.NodeList;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KubernetesServiceTest {

    @TempDir
    Path tempDir;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private KubernetesClient client;

    @Test
    void exportClusterInfoShouldCreateDirectoryWithFiles() throws IOException {
        var nodeList = createNodeList("test-node");
        when(client.nodes().list()).thenReturn(nodeList);

        var service = new KubernetesService(client, tempDir.toString(), "perf-demo", true);
        var result = service.exportClusterInfo();

        assertThat(result).isNotNull();
        var exportDir = Path.of(result);
        assertThat(Files.isDirectory(exportDir)).isTrue();
        assertThat(Files.exists(exportDir.resolve("nodes.json"))).isTrue();
    }

    @Test
    void exportClusterInfoShouldReturnNullOnException() throws IOException {
        // Create a file where the export directory would be, preventing directory creation
        Files.writeString(tempDir.resolve("kubernetes-export"), "blocking");

        var service = new KubernetesService(client, tempDir.toString(), "perf-demo", true);
        var result = service.exportClusterInfo();

        assertThat(result).isNull();
    }

    @Test
    void exportClusterInfoShouldHandleIndividualResourceFailures() throws IOException {
        when(client.nodes()).thenThrow(new RuntimeException("Connection refused"));

        var service = new KubernetesService(client, tempDir.toString(), "perf-demo", true);
        var result = service.exportClusterInfo();

        // Individual resource failures are caught per-resource; export still succeeds
        assertThat(result).isNotNull();
        assertThat(Files.isDirectory(Path.of(result))).isTrue();
    }

    @Test
    void exportClusterInfoShouldWriteFormattedJson() throws IOException {
        var nodeList = createNodeList("my-node");
        when(client.nodes().list()).thenReturn(nodeList);

        var service = new KubernetesService(client, tempDir.toString(), "perf-demo", true);
        var result = service.exportClusterInfo();

        assertThat(result).isNotNull();
        var nodesFile = Path.of(result, "nodes.json");
        assertThat(Files.exists(nodesFile)).isTrue();
        var content = Files.readString(nodesFile);
        assertThat(content).contains("my-node");
        assertThat(content).contains("\n");
        assertThat(content).contains("  ");
    }

    @Test
    void exportClusterInfoShouldHandleMultipleNodes() throws IOException {
        var nodeList = createNodeList("node-1", "node-2");
        when(client.nodes().list()).thenReturn(nodeList);

        var service = new KubernetesService(client, tempDir.toString(), "perf-demo", true);
        var result = service.exportClusterInfo();

        assertThat(result).isNotNull();
        var content = Files.readString(Path.of(result, "nodes.json"));
        assertThat(content).contains("node-1");
        assertThat(content).contains("node-2");
    }

    @Test
    void exportClusterInfoShouldContinueOnPartialFailure() throws IOException {
        var nodeList = createNodeList("test-node");
        when(client.nodes().list()).thenReturn(nodeList);
        when(client.namespaces().list()).thenThrow(new RuntimeException("Forbidden"));

        var service = new KubernetesService(client, tempDir.toString(), "perf-demo", true);
        var result = service.exportClusterInfo();

        assertThat(result).isNotNull();
        assertThat(Files.exists(Path.of(result, "nodes.json"))).isTrue();
    }

    @Test
    void exportClusterInfoShouldReturnNullWhenDisabled() {
        var service = new KubernetesService(client, tempDir.toString(), "perf-demo", false);
        var result = service.exportClusterInfo();

        assertThat(result).isNull();
    }

    private NodeList createNodeList(String... nodeNames) {
        var nodeList = new NodeList();
        var nodes = java.util.Arrays.stream(nodeNames)
                .map(this::createNode)
                .toList();
        nodeList.setItems(nodes);
        return nodeList;
    }

    private Node createNode(String name) {
        var node = new Node();
        var metadata = new ObjectMeta();
        metadata.setName(name);
        node.setMetadata(metadata);
        return node;
    }
}
