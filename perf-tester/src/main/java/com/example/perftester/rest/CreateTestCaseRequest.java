package com.example.perftester.rest;

import jakarta.validation.constraints.NotBlank;

public record CreateTestCaseRequest(@NotBlank String name, @NotBlank String message,
                                    Long headerTemplateId, Long responseTemplateId) {
}
