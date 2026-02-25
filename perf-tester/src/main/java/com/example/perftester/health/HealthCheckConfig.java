package com.example.perftester.health;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "health_check_config")
@Getter
@Setter
public class HealthCheckConfig {

    @Id
    private String service;

    private String host;

    private int port;

    private boolean enabled;

    private int connectionTimeoutMs;

    private int intervalMs;
}
