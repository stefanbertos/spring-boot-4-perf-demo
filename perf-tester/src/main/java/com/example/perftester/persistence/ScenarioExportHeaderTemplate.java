package com.example.perftester.persistence;

import java.util.List;

public record ScenarioExportHeaderTemplate(String name, List<TemplateFieldDto> fields) {
}
