package com.example.perftester.rest;

public class ExportOptions {
    private boolean exportGrafana;
    private boolean exportPrometheus;
    private boolean exportKubernetes;
    private boolean exportLogs;
    private boolean exportDatabase;

    @SuppressWarnings("PMD.UnnecessaryConstructor")
    public ExportOptions() {
        // Used by Spring MVC @ModelAttribute binding.
    }

    public boolean exportGrafana() {
        return exportGrafana;
    }

    public void setExportGrafana(boolean exportGrafana) {
        this.exportGrafana = exportGrafana;
    }

    public boolean exportPrometheus() {
        return exportPrometheus;
    }

    public void setExportPrometheus(boolean exportPrometheus) {
        this.exportPrometheus = exportPrometheus;
    }

    public boolean exportKubernetes() {
        return exportKubernetes;
    }

    public void setExportKubernetes(boolean exportKubernetes) {
        this.exportKubernetes = exportKubernetes;
    }

    public boolean exportLogs() {
        return exportLogs;
    }

    public void setExportLogs(boolean exportLogs) {
        this.exportLogs = exportLogs;
    }

    public boolean exportDatabase() {
        return exportDatabase;
    }

    public void setExportDatabase(boolean exportDatabase) {
        this.exportDatabase = exportDatabase;
    }
}
