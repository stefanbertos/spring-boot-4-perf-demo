package com.example.perftester.persistence;

import java.util.List;

public record ScenarioExportResponseTemplate(String name, List<ResponseFieldDto> fields) {
}
