package com.example.perftester.persistence;

public record ScenarioEntryRequest(Long testCaseId, int percentage, int displayOrder) {
}
