package com.example.perftester.kubernetes;

import com.fasterxml.jackson.databind.ObjectWriter;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.utils.Serialization;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Supplier;
import java.util.stream.Stream;

@Slf4j
@Service
public class KubernetesService {

    private final KubernetesClient client;
    private final String exportPath;
    private final String namespace;
    private final ObjectWriter prettyWriter;

    public KubernetesService(KubernetesClient client,
                             @Value("${app.export.path:./test-exports}") String exportPath,
                             @Value("${app.kubernetes.namespace:perf-demo}") String namespace) {
        this.client = client;
        this.exportPath = exportPath;
        this.namespace = namespace;
        this.prettyWriter = Serialization.jsonMapper().writerWithDefaultPrettyPrinter();
    }

    public String exportClusterInfo() {
        try {
            var exportDir = Path.of(exportPath, "kubernetes-export");
            Files.createDirectories(exportDir);

            exportClusterScopedResources(exportDir);
            exportNamespacedResources(exportDir);

            long fileCount;
            try (Stream<Path> files = Files.list(exportDir)) {
                fileCount = files.count();
            }
            log.info("Kubernetes cluster info exported: {} resource files to {}", fileCount, exportDir);
            return exportDir.toString();
        } catch (Exception e) {
            log.warn("Failed to export Kubernetes cluster info: {}. "
                    + "This is expected when not running in a Kubernetes cluster.", e.getMessage());
            return null;
        }
    }

    private void exportClusterScopedResources(Path exportDir) {
        exportResource(exportDir, "nodes.json", () -> client.nodes().list());
        exportResource(exportDir, "namespaces.json", () -> client.namespaces().list());
        exportResource(exportDir, "persistent-volumes.json", () -> client.persistentVolumes().list());
    }

    private void exportNamespacedResources(Path exportDir) {
        exportResource(exportDir, "pods.json",
                () -> client.pods().inNamespace(namespace).list());
        exportResource(exportDir, "deployments.json",
                () -> client.apps().deployments().inNamespace(namespace).list());
        exportResource(exportDir, "statefulsets.json",
                () -> client.apps().statefulSets().inNamespace(namespace).list());
        exportResource(exportDir, "daemonsets.json",
                () -> client.apps().daemonSets().inNamespace(namespace).list());
        exportResource(exportDir, "replicasets.json",
                () -> client.apps().replicaSets().inNamespace(namespace).list());
        exportResource(exportDir, "services.json",
                () -> client.services().inNamespace(namespace).list());
        exportResource(exportDir, "configmaps.json",
                () -> client.configMaps().inNamespace(namespace).list());
        exportResource(exportDir, "secrets.json",
                () -> client.secrets().inNamespace(namespace).list());
        exportResource(exportDir, "persistent-volume-claims.json",
                () -> client.persistentVolumeClaims().inNamespace(namespace).list());
        exportResource(exportDir, "ingresses.json",
                () -> client.network().v1().ingresses().inNamespace(namespace).list());
        exportResource(exportDir, "jobs.json",
                () -> client.batch().v1().jobs().inNamespace(namespace).list());
        exportResource(exportDir, "cronjobs.json",
                () -> client.batch().v1().cronjobs().inNamespace(namespace).list());
        exportResource(exportDir, "service-accounts.json",
                () -> client.serviceAccounts().inNamespace(namespace).list());
        exportResource(exportDir, "endpoints.json",
                () -> client.endpoints().inNamespace(namespace).list());
        exportResource(exportDir, "hpas.json",
                () -> client.autoscaling().v2().horizontalPodAutoscalers().inNamespace(namespace).list());
    }

    private void exportResource(Path exportDir, String filename, Supplier<Object> resourceSupplier) {
        try {
            var resource = resourceSupplier.get();
            var json = prettyWriter.writeValueAsString(resource);
            Files.writeString(exportDir.resolve(filename), json);
        } catch (Exception e) {
            log.warn("Failed to export {}: {}", filename, e.getMessage());
        }
    }
}
