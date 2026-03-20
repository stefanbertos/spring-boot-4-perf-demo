package com.example.perftester.perf;

import java.util.List;

public record ValidationResult(String testCaseName, boolean passed, List<String> failures) {
}
