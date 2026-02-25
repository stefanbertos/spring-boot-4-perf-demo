package com.example.perftester.health;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HealthCheckConfigServiceTest {

    @Mock
    private HealthCheckConfigRepository repository;

    @InjectMocks
    private HealthCheckConfigService service;

    private HealthCheckConfig config(String service, String host, int port, boolean enabled) {
        var cfg = new HealthCheckConfig();
        cfg.setService(service);
        cfg.setHost(host);
        cfg.setPort(port);
        cfg.setEnabled(enabled);
        cfg.setConnectionTimeoutMs(3000);
        cfg.setIntervalMs(30000);
        return cfg;
    }

    @Test
    void findAllShouldReturnMappedResponses() {
        when(repository.findAll()).thenReturn(List.of(
                config("kafka", "kafka-host", 9092, true),
                config("mq", "mq-host", 1414, false)
        ));

        var responses = service.findAll();

        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).service()).isEqualTo("kafka");
        assertThat(responses.get(0).host()).isEqualTo("kafka-host");
        assertThat(responses.get(0).port()).isEqualTo(9092);
        assertThat(responses.get(0).enabled()).isTrue();
        assertThat(responses.get(1).service()).isEqualTo("mq");
        assertThat(responses.get(1).enabled()).isFalse();
    }

    @Test
    void findAllShouldReturnEmptyListWhenNoConfigs() {
        when(repository.findAll()).thenReturn(List.of());

        assertThat(service.findAll()).isEmpty();
    }

    @Test
    void updateShouldModifyAndSaveConfig() {
        var existing = config("kafka", "old-host", 1111, false);
        when(repository.findById("kafka")).thenReturn(Optional.of(existing));
        when(repository.save(existing)).thenReturn(existing);

        var request = new HealthCheckConfigRequest("new-host", 9092, true, 1000, 5000);
        var response = service.update("kafka", request);

        assertThat(response.service()).isEqualTo("kafka");
        assertThat(response.host()).isEqualTo("new-host");
        assertThat(response.port()).isEqualTo(9092);
        assertThat(response.enabled()).isTrue();
        assertThat(response.connectionTimeoutMs()).isEqualTo(1000);
        assertThat(response.intervalMs()).isEqualTo(5000);
        verify(repository).save(existing);
    }

    @Test
    void updateShouldThrowNotFoundWhenServiceDoesNotExist() {
        when(repository.findById("unknown")).thenReturn(Optional.empty());

        var request = new HealthCheckConfigRequest("host", 9092, true, 1000, 5000);
        assertThatThrownBy(() -> service.update("unknown", request))
                .isInstanceOf(ResponseStatusException.class);
    }
}
