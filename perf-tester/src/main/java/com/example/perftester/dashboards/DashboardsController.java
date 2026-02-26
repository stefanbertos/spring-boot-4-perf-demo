package com.example.perftester.dashboards;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/dashboards")
@RequiredArgsConstructor
public class DashboardsController {

    private final DashboardsProperties dashboardsProperties;

    @GetMapping
    public List<DashboardLink> getDashboards() {
        return dashboardsProperties.dashboards().stream()
                .filter(DashboardLink::enabled)
                .toList();
    }
}
