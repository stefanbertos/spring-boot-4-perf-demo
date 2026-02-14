package com.example.apigateway.componenttest.containers;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.DynamicPropertyRegistrar;

@TestConfiguration
public class WireMockConfig {

    @Bean(destroyMethod = "stop")
    public WireMockServer perfTesterWireMock() {
        var server = new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort());
        server.start();
        return server;
    }

    @Bean
    public DynamicPropertyRegistrar wireMockPropertyRegistrar(WireMockServer perfTesterWireMock) {
        return registry -> {
            var baseUrl = "http://localhost:" + perfTesterWireMock.port();
            registry.add("PERF_TESTER_URL", () -> baseUrl);
            registry.add("CONFIG_SERVER_URL", () -> baseUrl);
        };
    }
}
