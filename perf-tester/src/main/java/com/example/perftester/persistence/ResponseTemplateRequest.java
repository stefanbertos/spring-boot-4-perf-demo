package com.example.perftester.persistence;

import java.util.List;

public record ResponseTemplateRequest(String name, List<ResponseFieldDto> fields) {
}
