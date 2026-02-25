package com.example.perftester.prometheus;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.prometheus")
public record PrometheusProperties(String url, String exportPath) {
}
