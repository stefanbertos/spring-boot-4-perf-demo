package com.example.perftester.rest;

import com.example.perftester.health.HealthCheckConfigRequest;
import com.example.perftester.health.HealthCheckConfigResponse;
import com.example.perftester.health.HealthCheckConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Admin: Health Checks", description = "Configure per-service health check endpoints monitored by the scheduler")
@RestController
@RequestMapping("/api/admin/healthcheck")
@RequiredArgsConstructor
public class HealthCheckAdminController {

    private final HealthCheckConfigService healthCheckConfigService;

    @Operation(summary = "List health check configurations for all services")
    @GetMapping
    public List<HealthCheckConfigResponse> list() {
        return healthCheckConfigService.findAll();
    }

    @Operation(summary = "Update health check configuration for a service")
    @PutMapping("/{service}")
    public HealthCheckConfigResponse update(
            @PathVariable String service,
            @Valid @RequestBody HealthCheckConfigRequest request
    ) {
        return healthCheckConfigService.update(service, request);
    }
}
