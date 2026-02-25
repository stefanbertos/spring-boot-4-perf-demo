package com.example.perftester.grafana;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.grafana")
public record GrafanaProperties(String url, String exportPath, String apiKey) {
}
