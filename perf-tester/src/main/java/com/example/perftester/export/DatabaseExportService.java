package com.example.perftester.export;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class DatabaseExportService {

    private final DbExportQueryRepository repository;
    private final JdbcTemplate jdbcTemplate;

    @Transactional(readOnly = true)
    public List<DbExportQueryResponse> findAll() {
        return repository.findAllByOrderByDisplayOrderAscNameAsc().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public DbExportQueryResponse create(DbExportQueryRequest request) {
        validateSelectQuery(request.sqlQuery());
        var entity = new DbExportQuery();
        entity.setName(request.name());
        entity.setSqlQuery(request.sqlQuery());
        entity.setDisplayOrder(request.displayOrder());
        return toResponse(repository.save(entity));
    }

    @Transactional
    public DbExportQueryResponse update(Long id, DbExportQueryRequest request) {
        validateSelectQuery(request.sqlQuery());
        var entity = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "DB export query not found: " + id));
        entity.setName(request.name());
        entity.setSqlQuery(request.sqlQuery());
        entity.setDisplayOrder(request.displayOrder());
        return toResponse(repository.save(entity));
    }

    @Transactional
    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "DB export query not found: " + id);
        }
        repository.deleteById(id);
    }

    public Map<String, Path> executeExportQueries() {
        var results = new LinkedHashMap<String, Path>();
        var queries = repository.findAllByOrderByDisplayOrderAscNameAsc();
        for (var query : queries) {
            try {
                results.put(query.getName(), executeQueryAsCsv(query.getSqlQuery()));
            } catch (Exception e) {
                log.warn("Failed to execute export query '{}': {}", query.getName(), e.getMessage());
            }
        }
        return results;
    }

    private Path executeQueryAsCsv(String sql) throws IOException {
        var tempFile = Files.createTempFile("db-export-", ".csv");
        try (var writer = Files.newBufferedWriter(tempFile, StandardCharsets.UTF_8)) {
            jdbcTemplate.query(
                    conn -> {
                        var ps = conn.prepareStatement(sql,
                                ResultSet.TYPE_FORWARD_ONLY,
                                ResultSet.CONCUR_READ_ONLY);
                        ps.setFetchSize(1000);
                        return ps;
                    },
                    (ResultSetExtractor<Void>) rs -> {
                        try {
                            var meta = rs.getMetaData();
                            var cols = meta.getColumnCount();
                            writeHeader(writer, meta, cols);
                            while (rs.next()) {
                                writeRow(writer, rs, cols);
                            }
                        } catch (IOException e) {
                            throw new UncheckedIOException(e);
                        }
                        return null;
                    });
        }
        return tempFile;
    }

    private void writeHeader(BufferedWriter writer, ResultSetMetaData meta, int cols)
            throws SQLException, IOException {
        for (int i = 1; i <= cols; i++) {
            if (i > 1) {
                writer.write(',');
            }
            writer.write(escapeCsv(meta.getColumnLabel(i)));
        }
        writer.newLine();
    }

    private void writeRow(BufferedWriter writer, ResultSet rs, int cols)
            throws SQLException, IOException {
        for (int i = 1; i <= cols; i++) {
            if (i > 1) {
                writer.write(',');
            }
            var val = rs.getString(i);
            writer.write(val != null ? escapeCsv(val) : "");
        }
        writer.newLine();
    }

    private String escapeCsv(String value) {
        if (value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    private DbExportQueryResponse toResponse(DbExportQuery entity) {
        return new DbExportQueryResponse(entity.getId(), entity.getName(),
                entity.getSqlQuery(), entity.getDisplayOrder());
    }

    private void validateSelectQuery(String sql) {
        if (!sql.trim().toUpperCase().startsWith("SELECT")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Only SELECT queries are allowed");
        }
    }
}
