package com.example.perftester.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.export")
public record ExportProperties(String path) {
}
