package com.example.perftester.persistence;

import java.util.List;

public record HeaderTemplateRequest(String name, List<TemplateFieldDto> fields) {
}
