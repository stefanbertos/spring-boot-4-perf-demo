package com.example.perftester.health;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HealthCheckPropertiesTest {

    @Test
    void shouldCreatePropertiesWithAllValues() {
        var kafka = new HealthCheckProperties.ServiceEndpoint("kafka-host", 9092);
        var mq = new HealthCheckProperties.ServiceEndpoint("mq-host", 1414);
        var redis = new HealthCheckProperties.ServiceEndpoint("redis-host", 6379);

        var properties = new HealthCheckProperties(kafka, mq, redis, 3000, 30000);

        assertThat(properties.kafka().host()).isEqualTo("kafka-host");
        assertThat(properties.kafka().port()).isEqualTo(9092);
        assertThat(properties.mq().host()).isEqualTo("mq-host");
        assertThat(properties.mq().port()).isEqualTo(1414);
        assertThat(properties.redis().host()).isEqualTo("redis-host");
        assertThat(properties.redis().port()).isEqualTo(6379);
        assertThat(properties.connectionTimeoutMs()).isEqualTo(3000);
        assertThat(properties.intervalMs()).isEqualTo(30000);
    }

    @Test
    void serviceEndpointShouldStoreHostAndPort() {
        var endpoint = new HealthCheckProperties.ServiceEndpoint("test-host", 8080);

        assertThat(endpoint.host()).isEqualTo("test-host");
        assertThat(endpoint.port()).isEqualTo(8080);
    }

    @Test
    void redisDefaultValuesShouldBeLocalhostAndDefaultPort() {
        var redis = new HealthCheckProperties.ServiceEndpoint("localhost", 6379);

        assertThat(redis.host()).isEqualTo("localhost");
        assertThat(redis.port()).isEqualTo(6379);
    }
}
