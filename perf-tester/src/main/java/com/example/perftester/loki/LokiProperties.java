package com.example.perftester.loki;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.loki")
public record LokiProperties(String url) {
}
