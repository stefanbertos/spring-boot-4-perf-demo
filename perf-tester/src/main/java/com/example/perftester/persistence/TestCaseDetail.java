package com.example.perftester.persistence;

import java.time.Instant;

public record TestCaseDetail(long id, String name, String message,
                             Long headerTemplateId, String headerTemplateName,
                             Long responseTemplateId, String responseTemplateName,
                             Instant createdAt, Instant updatedAt) {
}
