package com.example.perftester.export;

public record DbExportQueryResponse(
        Long id,
        String name,
        String sqlQuery,
        int displayOrder
) {
}
