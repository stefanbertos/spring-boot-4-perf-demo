package com.example.perftester.kubernetes;

import com.example.perftester.config.ExportProperties;
import com.fasterxml.jackson.databind.ObjectWriter;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.utils.Serialization;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;

@Slf4j
@Service
public class KubernetesService {

    private final KubernetesClient client;
    private final String exportPath;
    private final String namespace;
    private final boolean enabled;
    private final ObjectWriter prettyWriter;

    public KubernetesService(KubernetesClient client,
                             ExportProperties exportProperties,
                             KubernetesProperties kubernetesProperties) {
        this.client = client;
        this.exportPath = exportProperties.path();
        this.namespace = kubernetesProperties.namespace();
        this.enabled = kubernetesProperties.exportEnabled();
        this.prettyWriter = Serialization.jsonMapper().writerWithDefaultPrettyPrinter();
    }

    /**
     * Exports Kubernetes cluster information (nodes, pods, services, deployments, configmaps)
     * from the {@code perf-demo} namespace to JSON files in the configured export directory.
     *
     * @return the path to the export directory, or {@code null} if the export failed
     */
    public String exportClusterInfo() {
        if (!enabled) {
            log.info("Kubernetes export is disabled (app.kubernetes.export-enabled=false)");
            return null;
        }
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

    public List<NamespaceInfo> listNamespaces() {
        try {
            return client.namespaces().list().getItems().stream()
                    .map(n -> new NamespaceInfo(n.getMetadata().getName(),
                            n.getStatus() != null ? n.getStatus().getPhase() : "Unknown"))
                    .toList();
        } catch (Exception e) {
            log.warn("Could not list Kubernetes namespaces: {}. Not running in a cluster?", e.getMessage());
            return List.of();
        }
    }

    public List<DeploymentInfo> listDeployments(String ns) {
        try {
            var items = client.apps().deployments().inNamespace(ns).list().getItems();
            return items.stream()
                    .map(d -> {
                        var spec = d.getSpec();
                        var status = d.getStatus();
                        var desired = spec != null && spec.getReplicas() != null ? spec.getReplicas() : 0;
                        var ready = status != null && status.getReadyReplicas() != null ? status.getReadyReplicas() : 0;
                        return new DeploymentInfo(d.getMetadata().getName(), ns, desired, ready);
                    })
                    .toList();
        } catch (Exception e) {
            log.warn("Could not list deployments in namespace {}: {}. Not running in a cluster?", ns, e.getMessage());
            return List.of();
        }
    }

    public void scaleDeployment(String name, int replicas) {
        scaleDeployment(name, namespace, replicas);
    }

    public void scaleDeployment(String name, String ns, int replicas) {
        client.apps().deployments().inNamespace(ns).withName(name).scale(replicas);
        log.info("Scaled deployment {} in namespace {} to {} replicas", name, ns, replicas);
    }

    public record NamespaceInfo(String name, String phase) {
    }

    public record DeploymentInfo(String name, String namespace, int desiredReplicas, int readyReplicas) {
    }
}
