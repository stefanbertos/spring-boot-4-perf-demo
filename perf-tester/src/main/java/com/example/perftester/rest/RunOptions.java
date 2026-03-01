package com.example.perftester.rest;

public class RunOptions {
    private String testId;
    private boolean debug;
    private Long scenarioId;

    public RunOptions() {
        // Used by Spring MVC @ModelAttribute binding.
    }

    public RunOptions(String testId, boolean debug, Long scenarioId) {
        this.testId = testId;
        this.debug = debug;
        this.scenarioId = scenarioId;
    }

    public String testId() {
        return testId;
    }

    public void setTestId(String testId) {
        this.testId = testId;
    }

    public boolean debug() {
        return debug;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    public Long scenarioId() {
        return scenarioId;
    }

    public void setScenarioId(Long scenarioId) {
        this.scenarioId = scenarioId;
    }
}
