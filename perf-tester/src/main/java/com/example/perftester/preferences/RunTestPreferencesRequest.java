package com.example.perftester.preferences;

public record RunTestPreferencesRequest(
        boolean exportGrafana,
        boolean exportPrometheus,
        boolean exportKubernetes,
        boolean exportLogs,
        boolean exportDatabase,
        boolean debug) {
}
