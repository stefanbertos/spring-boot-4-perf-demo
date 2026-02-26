package com.example.perftester.dashboards;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "app")
public record DashboardsProperties(List<DashboardLink> dashboards) {
}
