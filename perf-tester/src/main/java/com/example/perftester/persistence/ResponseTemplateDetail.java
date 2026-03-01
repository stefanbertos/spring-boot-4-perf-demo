package com.example.perftester.persistence;

import java.util.List;

public record ResponseTemplateDetail(Long id, String name, List<ResponseFieldDto> fields,
                                     String createdAt, String updatedAt) {
}
