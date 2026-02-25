package com.example.perftester.export;

import jakarta.validation.constraints.NotBlank;

public record DbExportQueryRequest(
        @NotBlank String name,
        @NotBlank String sqlQuery,
        int displayOrder
) {
}
