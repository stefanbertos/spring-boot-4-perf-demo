package com.example.perftester.rest;

import java.util.List;

import com.example.perftester.kubernetes.KubernetesService;
import com.example.perftester.kubernetes.KubernetesService.DeploymentInfo;
import com.example.perftester.kubernetes.KubernetesService.NamespaceInfo;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@Validated
@RestController
@RequestMapping("/api/admin/kubernetes")
@RequiredArgsConstructor
public class KubernetesAdminController {

    private final KubernetesService kubernetesService;

    @GetMapping("/namespaces/list")
    public ResponseEntity<List<NamespaceInfo>> listNamespaces() {
        return ResponseEntity.ok(kubernetesService.listNamespaces());
    }

    @GetMapping("/deployments/list")
    public ResponseEntity<List<DeploymentInfo>> listDeployments(@RequestParam @NotBlank String namespace) {
        return ResponseEntity.ok(kubernetesService.listDeployments(namespace));
    }

    @PostMapping("/deployments/scale")
    public ResponseEntity<Void> scaleDeployment(
            @RequestParam @NotBlank String name,
            @RequestParam @NotBlank String namespace,
            @RequestParam @Min(0) int replicas) {
        kubernetesService.scaleDeployment(name, namespace, replicas);
        return ResponseEntity.noContent().build();
    }
}
