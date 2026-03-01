package com.example.perftester.export;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DatabaseExportServiceTest {

    @Mock
    private DbExportQueryRepository repository;

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private DatabaseExportService databaseExportService;

    private DbExportQuery queryWithId(Long id) {
        var query = new DbExportQuery();
        query.setId(id);
        query.setName("query-" + id);
        query.setSqlQuery("SELECT 1 FROM DUAL");
        query.setDisplayOrder(0);
        return query;
    }

    @Test
    void findAllShouldReturnMappedResponses() {
        when(repository.findAllByOrderByDisplayOrderAscNameAsc()).thenReturn(List.of(queryWithId(1L)));

        var result = databaseExportService.findAll();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo(1L);
        assertThat(result.get(0).name()).isEqualTo("query-1");
        assertThat(result.get(0).sqlQuery()).isEqualTo("SELECT 1 FROM DUAL");
    }

    @Test
    void createShouldSaveAndReturnResponse() {
        var request = new DbExportQueryRequest("my-query", "SELECT * FROM test_run", 1);
        var saved = queryWithId(2L);
        saved.setName("my-query");
        saved.setSqlQuery("SELECT * FROM test_run");
        saved.setDisplayOrder(1);
        when(repository.save(any())).thenReturn(saved);

        var result = databaseExportService.create(request);

        var captor = ArgumentCaptor.forClass(DbExportQuery.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getName()).isEqualTo("my-query");
        assertThat(result.name()).isEqualTo("my-query");
    }

    @Test
    void createShouldRejectNonSelectQuery() {
        var request = new DbExportQueryRequest("bad-query", "DELETE FROM test_run", 0);

        assertThatThrownBy(() -> databaseExportService.create(request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("SELECT");
    }

    @Test
    void createShouldAcceptSelectWithLeadingWhitespace() {
        var request = new DbExportQueryRequest("ok-query", "  select id from test_run", 0);
        var saved = queryWithId(3L);
        when(repository.save(any())).thenReturn(saved);

        var result = databaseExportService.create(request);

        assertThat(result).isNotNull();
    }

    @Test
    void updateShouldModifyAndReturn() {
        var entity = queryWithId(1L);
        var request = new DbExportQueryRequest("updated", "SELECT 1 FROM DUAL", 2);
        var saved = queryWithId(1L);
        saved.setName("updated");
        when(repository.findById(1L)).thenReturn(Optional.of(entity));
        when(repository.save(entity)).thenReturn(saved);

        var result = databaseExportService.update(1L, request);

        assertThat(result.name()).isEqualTo("updated");
    }

    @Test
    void updateShouldThrowWhenNotFound() {
        var request = new DbExportQueryRequest("name", "SELECT 1 FROM DUAL", 0);
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> databaseExportService.update(99L, request))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void updateShouldRejectNonSelectQuery() {
        var request = new DbExportQueryRequest("name", "INSERT INTO foo VALUES (1)", 0);

        assertThatThrownBy(() -> databaseExportService.update(1L, request))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void deleteShouldCallDeleteById() {
        when(repository.existsById(1L)).thenReturn(true);

        databaseExportService.delete(1L);

        verify(repository).deleteById(1L);
    }

    @Test
    void deleteShouldThrowWhenNotFound() {
        when(repository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> databaseExportService.delete(99L))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void executeExportQueriesShouldReturnEmptyWhenNoQueries() {
        when(repository.findAllByOrderByDisplayOrderAscNameAsc()).thenReturn(List.of());

        var result = databaseExportService.executeExportQueries();

        assertThat(result).isEmpty();
    }

    @Test
    void executeExportQueriesShouldSkipFailingQuery() {
        var query = queryWithId(1L);
        when(repository.findAllByOrderByDisplayOrderAscNameAsc()).thenReturn(List.of(query));
        doThrow(new RuntimeException("DB error"))
                .when(jdbcTemplate)
                .query(any(PreparedStatementCreator.class), ArgumentMatchers.<ResultSetExtractor<Void>>any());

        var result = databaseExportService.executeExportQueries();

        assertThat(result).isEmpty();
    }
}
