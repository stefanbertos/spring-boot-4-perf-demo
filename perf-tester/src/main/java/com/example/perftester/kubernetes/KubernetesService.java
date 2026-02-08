package com.example.perftester.kubernetes;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.utils.Serialization;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;

@Slf4j
@Service
public class KubernetesService {

    private final KubernetesClient client;
    private final String exportPath;

    public KubernetesService(KubernetesClient client,
                             @Value("${app.export.path:./test-exports}") String exportPath) {
        this.client = client;
        this.exportPath = exportPath;
    }

    public String exportNodeInfo() {
        try {
            var nodeList = client.nodes().list();
            log.info("Retrieved information for {} Kubernetes nodes", nodeList.getItems().size());

            Files.createDirectories(Path.of(exportPath));
            var exportFile = Path.of(exportPath).resolve("kubernetes-nodes.json");
            Files.writeString(exportFile, Serialization.asJson(nodeList));
            log.info("Kubernetes node info exported to: {}", exportFile);
            return exportFile.toString();
        } catch (Exception e) {
            log.warn("Failed to export Kubernetes node info: {}. "
                    + "This is expected when not running in a Kubernetes cluster.", e.getMessage());
            return null;
        }
    }
}
