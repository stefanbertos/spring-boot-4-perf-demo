package com.example.perftester.persistence;

public class TestScenarioNotFoundException extends RuntimeException {

    public TestScenarioNotFoundException(Long id) {
        super("Test scenario not found: " + id);
    }
}
