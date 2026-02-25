package com.example.perftester.kubernetes;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.kubernetes")
public record KubernetesProperties(String namespace, boolean exportEnabled) {
}
