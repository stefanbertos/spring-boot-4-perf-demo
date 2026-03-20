package com.example.perftester.persistence;

public record ScenarioExportEntry(
        String testCaseName,
        String message,
        int percentage,
        int displayOrder,
        ScenarioExportHeaderTemplate headerTemplate,
        ScenarioExportResponseTemplate responseTemplate) {
}
