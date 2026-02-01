package com.example.perftester.health;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HealthCheckPropertiesTest {

    @Test
    void shouldCreatePropertiesWithAllValues() {
        var kafka = new HealthCheckProperties.ServiceEndpoint("kafka-host", 9092);
        var mq = new HealthCheckProperties.ServiceEndpoint("mq-host", 1414);
        var oracle = new HealthCheckProperties.ServiceEndpoint("oracle-host", 1521);

        var properties = new HealthCheckProperties(kafka, mq, oracle, 3000, 30000);

        assertThat(properties.kafka().host()).isEqualTo("kafka-host");
        assertThat(properties.kafka().port()).isEqualTo(9092);
        assertThat(properties.mq().host()).isEqualTo("mq-host");
        assertThat(properties.mq().port()).isEqualTo(1414);
        assertThat(properties.oracle().host()).isEqualTo("oracle-host");
        assertThat(properties.oracle().port()).isEqualTo(1521);
        assertThat(properties.connectionTimeoutMs()).isEqualTo(3000);
        assertThat(properties.intervalMs()).isEqualTo(30000);
    }

    @Test
    void serviceEndpointShouldStoreHostAndPort() {
        var endpoint = new HealthCheckProperties.ServiceEndpoint("test-host", 8080);

        assertThat(endpoint.host()).isEqualTo("test-host");
        assertThat(endpoint.port()).isEqualTo(8080);
    }
}
