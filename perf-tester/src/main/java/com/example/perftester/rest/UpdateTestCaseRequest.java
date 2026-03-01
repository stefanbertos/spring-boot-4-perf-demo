package com.example.perftester.rest;

import jakarta.validation.constraints.NotBlank;

public record UpdateTestCaseRequest(@NotBlank String name, @NotBlank String message,
                                    Long headerTemplateId, Long responseTemplateId) {
}
