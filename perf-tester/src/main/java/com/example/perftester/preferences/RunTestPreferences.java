package com.example.perftester.preferences;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "run_test_preferences")
@Getter
@Setter
class RunTestPreferences {

    @Id
    private Long id;
    private boolean exportGrafana;
    private boolean exportPrometheus;
    private boolean exportKubernetes;
    private boolean exportLogs;
    private boolean exportDatabase;
    private boolean debug;
}
