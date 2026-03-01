package com.example.perftester.persistence;

import java.util.List;

public record HeaderTemplateDetail(Long id, String name, List<TemplateFieldDto> fields,
                                   String createdAt, String updatedAt) {
}
