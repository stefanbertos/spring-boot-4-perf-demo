package com.example.perftester.rest;

import com.example.perftester.export.DatabaseExportService;
import com.example.perftester.export.DbExportQueryRequest;
import com.example.perftester.export.DbExportQueryResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Admin: DB Export Queries", description = "Manage custom SQL SELECT queries executed after a performance test to export database state")
@RestController
@RequestMapping("/api/admin/db-queries")
@RequiredArgsConstructor
public class DbExportQueryController {

    private final DatabaseExportService databaseExportService;

    @Operation(summary = "List all database export queries")
    @GetMapping
    public List<DbExportQueryResponse> listAll() {
        return databaseExportService.findAll();
    }

    @Operation(summary = "Create a database export query", description = "Only SELECT statements are accepted")
    @PostMapping
    public ResponseEntity<DbExportQueryResponse> create(@Valid @RequestBody DbExportQueryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(databaseExportService.create(request));
    }

    @Operation(summary = "Update a database export query")
    @PutMapping("/{id}")
    public DbExportQueryResponse update(@PathVariable Long id,
                                        @Valid @RequestBody DbExportQueryRequest request) {
        return databaseExportService.update(id, request);
    }

    @Operation(summary = "Delete a database export query")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        databaseExportService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
