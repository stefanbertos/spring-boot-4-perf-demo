package com.example.perftester.rest;

import com.example.perftester.health.HealthCheckConfigRequest;
import com.example.perftester.health.HealthCheckConfigResponse;
import com.example.perftester.health.HealthCheckConfigService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/healthcheck")
@RequiredArgsConstructor
public class HealthCheckAdminController {

    private final HealthCheckConfigService healthCheckConfigService;

    @GetMapping
    public List<HealthCheckConfigResponse> list() {
        return healthCheckConfigService.findAll();
    }

    @PutMapping("/{service}")
    public HealthCheckConfigResponse update(
            @PathVariable String service,
            @Valid @RequestBody HealthCheckConfigRequest request
    ) {
        return healthCheckConfigService.update(service, request);
    }
}
