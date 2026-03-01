package com.example.perftester.rest;

import java.time.Instant;

public record TestCaseResponse(long id, String name, String message,
                               Long headerTemplateId, String headerTemplateName,
                               Long responseTemplateId, String responseTemplateName,
                               Instant createdAt, Instant updatedAt) {
}
