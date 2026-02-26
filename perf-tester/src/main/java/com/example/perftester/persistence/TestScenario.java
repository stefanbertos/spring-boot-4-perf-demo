package com.example.perftester.persistence;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "test_scenario")
@Getter
@Setter
public class TestScenario {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "test_scenario_seq")
    @SequenceGenerator(name = "test_scenario_seq", sequenceName = "test_scenario_seq", allocationSize = 1)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(name = "count", nullable = false)
    private int count = 100;

    @OneToMany(mappedBy = "scenario", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ScenarioTestCase> entries = new ArrayList<>();

    @Column(name = "scheduled_enabled", nullable = false)
    private boolean scheduledEnabled;

    @Column(name = "scheduled_time", length = 5)
    private String scheduledTime;

    @Column(name = "warmup_count", nullable = false)
    private int warmupCount;

    @Column(name = "test_type", length = 20)
    private String testType;

    @Column(name = "think_time", columnDefinition = "text")
    private String thinkTimeJson;

    @Column(name = "thresholds", columnDefinition = "text")
    private String thresholdsJson;

    @Column(name = "infra_profile_id")
    private Long infraProfileId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        var now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}
