package com.example.perftester.export;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

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

    public Map<String, String> executeExportQueries() {
        var results = new LinkedHashMap<String, String>();
        var queries = repository.findAllByOrderByDisplayOrderAscNameAsc();
        for (var query : queries) {
            try {
                results.put(query.getName(), executeQueryAsCsv(query.getSqlQuery()));
            } catch (Exception e) {
                log.warn("Failed to execute export query '{}': {}", query.getName(), e.getMessage());
                results.put(query.getName(), "ERROR: " + e.getMessage());
            }
        }
        return results;
    }

    private String executeQueryAsCsv(String sql) {
        var sb = new StringBuilder();
        jdbcTemplate.query(sql, (ResultSetExtractor<Void>) rs -> {
            var meta = rs.getMetaData();
            var cols = meta.getColumnCount();
            writeHeader(sb, meta, cols);
            while (rs.next()) {
                writeRow(sb, rs, cols);
            }
            return null;
        });
        return sb.toString();
    }

    private void writeHeader(StringBuilder sb, ResultSetMetaData meta, int cols) throws SQLException {
        for (int i = 1; i <= cols; i++) {
            if (i > 1) {
                sb.append(',');
            }
            sb.append(escapeCsv(meta.getColumnLabel(i)));
        }
        sb.append('\n');
    }

    private void writeRow(StringBuilder sb, ResultSet rs, int cols) throws SQLException {
        for (int i = 1; i <= cols; i++) {
            if (i > 1) {
                sb.append(',');
            }
            var val = rs.getString(i);
            sb.append(val != null ? escapeCsv(val) : "");
        }
        sb.append('\n');
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
