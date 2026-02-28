package com.example.perftester.dashboards;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Dashboards", description = "Retrieve links to configured monitoring dashboards (Grafana, Prometheus, etc.)")
@RestController
@RequestMapping("/api/dashboards")
@RequiredArgsConstructor
public class DashboardsController {

    private final DashboardsProperties dashboardsProperties;

    @Operation(summary = "List available monitoring dashboards")
    @GetMapping
    public List<DashboardLink> getDashboards() {
        return dashboardsProperties.dashboards().stream()
                .filter(DashboardLink::enabled)
                .toList();
    }
}
