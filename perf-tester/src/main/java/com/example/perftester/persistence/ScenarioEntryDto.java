package com.example.perftester.persistence;

public record ScenarioEntryDto(Long id, Long testCaseId, String testCaseName,
                               int percentage, int displayOrder) {
}
