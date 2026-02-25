package com.example.perftester.health;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class HealthCheckConfigService {

    private final HealthCheckConfigRepository repository;

    @Transactional(readOnly = true)
    public List<HealthCheckConfigResponse> findAll() {
        return repository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public HealthCheckConfigResponse update(String service, HealthCheckConfigRequest request) {
        var config = repository.findById(service)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown service: " + service));
        config.setHost(request.host());
        config.setPort(request.port());
        config.setEnabled(request.enabled());
        config.setConnectionTimeoutMs(request.connectionTimeoutMs());
        config.setIntervalMs(request.intervalMs());
        return toResponse(repository.save(config));
    }

    private HealthCheckConfigResponse toResponse(HealthCheckConfig config) {
        return new HealthCheckConfigResponse(
                config.getService(),
                config.getHost(),
                config.getPort(),
                config.isEnabled(),
                config.getConnectionTimeoutMs(),
                config.getIntervalMs()
        );
    }
}
