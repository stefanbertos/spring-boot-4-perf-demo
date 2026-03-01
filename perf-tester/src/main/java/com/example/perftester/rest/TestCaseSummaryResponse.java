package com.example.perftester.rest;

import java.time.Instant;

public record TestCaseSummaryResponse(long id, String name,
                                      Long headerTemplateId, String headerTemplateName,
                                      Long responseTemplateId, String responseTemplateName,
                                      Instant updatedAt) {
}
