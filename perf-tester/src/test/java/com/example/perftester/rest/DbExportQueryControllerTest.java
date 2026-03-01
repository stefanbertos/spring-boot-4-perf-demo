package com.example.perftester.rest;

import com.example.perftester.export.DatabaseExportService;
import com.example.perftester.export.DbExportQueryRequest;
import com.example.perftester.export.DbExportQueryResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DbExportQueryControllerTest {

    @Mock
    private DatabaseExportService databaseExportService;

    @InjectMocks
    private DbExportQueryController controller;

    private DbExportQueryResponse response(Long id) {
        return new DbExportQueryResponse(id, "query-" + id, "SELECT 1 FROM DUAL", 0);
    }

    @Test
    void listAllShouldReturnAllQueries() {
        when(databaseExportService.findAll()).thenReturn(List.of(response(1L), response(2L)));

        var result = controller.listAll();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).id()).isEqualTo(1L);
        assertThat(result.get(1).id()).isEqualTo(2L);
    }

    @Test
    void createShouldReturnCreatedStatus() {
        var request = new DbExportQueryRequest("my-query", "SELECT * FROM test_run", 1);
        var saved = new DbExportQueryResponse(3L, "my-query", "SELECT * FROM test_run", 1);
        when(databaseExportService.create(request)).thenReturn(saved);

        var result = controller.create(request);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody().id()).isEqualTo(3L);
        assertThat(result.getBody().name()).isEqualTo("my-query");
    }

    @Test
    void updateShouldReturnUpdatedQuery() {
        var request = new DbExportQueryRequest("updated", "SELECT id FROM test_run", 2);
        var updated = new DbExportQueryResponse(1L, "updated", "SELECT id FROM test_run", 2);
        when(databaseExportService.update(1L, request)).thenReturn(updated);

        var result = controller.update(1L, request);

        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.name()).isEqualTo("updated");
    }

    @Test
    void deleteShouldReturnNoContent() {
        var result = controller.delete(1L);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(databaseExportService).delete(1L);
    }
}
