package com.example.perftester.rest;

import com.example.perftester.loki.LogEntry;
import com.example.perftester.loki.LokiService;
import com.example.perftester.persistence.TestRun;
import com.example.perftester.persistence.TestRunNotFoundException;
import com.example.perftester.persistence.TestRunService;
import com.example.perftester.persistence.TestRunSnapshot;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.HttpStatus;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TestRunControllerTest {

    @Mock
    private TestRunService testRunService;

    @Mock
    private LokiService lokiService;

    @InjectMocks
    private TestRunController controller;

    private TestRun runWithId(Long id) {
        var run = new TestRun();
        run.setId(id);
        run.setTestRunId("uuid-" + id);
        run.setTestId("test-id");
        run.setStatus("COMPLETED");
        run.setMessageCount(100);
        run.setStartedAt(Instant.now());
        return run;
    }

    @Test
    void listAllShouldReturnPagedRuns() {
        when(testRunService.findAll(0, 20, null))
                .thenReturn(new PageImpl<>(List.of(runWithId(1L), runWithId(2L))));

        var result = controller.listAll(0, 20, null);

        assertThat(result.content()).hasSize(2);
        assertThat(result.content().get(0).id()).isEqualTo(1L);
        assertThat(result.content().get(1).id()).isEqualTo(2L);
        assertThat(result.totalElements()).isEqualTo(2L);
    }

    @Test
    void getByIdShouldReturnDetailResponse() {
        when(testRunService.findById(1L)).thenReturn(runWithId(1L));

        var result = controller.getById(1L);

        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.testRunId()).isEqualTo("uuid-1");
        assertThat(result.status()).isEqualTo("COMPLETED");
        assertThat(result.tags()).isEmpty();
    }

    @Test
    void getByIdShouldPropagateNotFound() {
        when(testRunService.findById(99L)).thenThrow(new TestRunNotFoundException(99L));

        assertThatThrownBy(() -> controller.getById(99L))
                .isInstanceOf(TestRunNotFoundException.class);
    }

    @Test
    void deleteShouldDelegateToService() {
        controller.delete(1L);
        verify(testRunService).delete(1L);
    }

    @Test
    void bulkDeleteShouldDelegateToService() {
        controller.bulkDelete(new BulkDeleteRequest(List.of(1L, 2L)));
        verify(testRunService).bulkDelete(List.of(1L, 2L));
    }

    @Test
    void setTagsShouldDelegateToService() {
        controller.setTags(1L, new SetTagsRequest(List.of("smoke", "before-deploy")));
        verify(testRunService).setTags(1L, List.of("smoke", "before-deploy"));
    }

    @Test
    void getAllTagsShouldDelegateToService() {
        when(testRunService.getAllUniqueTags()).thenReturn(List.of("a", "b"));

        var result = controller.getAllTags();

        assertThat(result).containsExactly("a", "b");
    }

    @Test
    void getTrendsShouldReturnMappedPoints() {
        var run = runWithId(1L);
        run.setTps(50.0);
        run.setP99LatencyMs(100.0);
        when(testRunService.getTrendData()).thenReturn(List.of(run));

        var result = controller.getTrends();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).tps()).isEqualTo(50.0);
        assertThat(result.get(0).p99LatencyMs()).isEqualTo(100.0);
    }

    @Test
    void downloadZipShouldReturnNotFoundWhenPathIsNull() {
        var run = runWithId(1L);
        when(testRunService.findById(1L)).thenReturn(run);

        var response = controller.downloadZip(1L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void downloadZipShouldReturnNotFoundWhenFileDoesNotExist() {
        var run = runWithId(1L);
        run.setZipFilePath("/nonexistent/path/export.zip");
        when(testRunService.findById(1L)).thenReturn(run);

        var response = controller.downloadZip(1L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void downloadZipShouldReturnOkWhenFileExists(@TempDir Path tempDir) throws IOException {
        var zipFile = Files.createTempFile(tempDir, "export", ".zip");
        Files.writeString(zipFile, "zip content");
        var run = runWithId(1L);
        run.setZipFilePath(zipFile.toString());
        when(testRunService.findById(1L)).thenReturn(run);

        var response = controller.downloadZip(1L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
    }

    @Test
    void getLogsShouldQueryLokiWithRunTimeWindow() {
        var start = Instant.parse("2024-01-01T10:00:00Z");
        var end = Instant.parse("2024-01-01T10:05:00Z");
        var run = runWithId(1L);
        run.setStartedAt(start);
        run.setCompletedAt(end);
        var entries = List.of(new LogEntry("2024-01-01T10:01:00Z", "INFO", "Test started"));
        when(testRunService.findById(1L)).thenReturn(run);
        when(lokiService.queryLogs(start, end)).thenReturn(entries);

        var result = controller.getLogs(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).message()).isEqualTo("Test started");
    }

    @Test
    void getLogsShouldUseNowWhenCompletedAtIsNull() {
        var run = runWithId(1L);
        run.setStartedAt(Instant.now().minusSeconds(60));
        when(testRunService.findById(1L)).thenReturn(run);
        when(lokiService.queryLogs(any(Instant.class), any(Instant.class))).thenReturn(List.of());

        var result = controller.getLogs(1L);

        assertThat(result).isEmpty();
    }

    @Test
    void getSnapshotsShouldReturnMappedSnapshots() {
        var snapshot = new TestRunSnapshot();
        snapshot.setId(10L);
        snapshot.setTestRunId(1L);
        snapshot.setSampledAt(Instant.now());
        snapshot.setOutboundQueueDepth(5);
        snapshot.setInboundQueueDepth(3);
        snapshot.setKafkaRequestsLag(10L);
        snapshot.setKafkaResponsesLag(2L);
        when(testRunService.findSnapshots(1L)).thenReturn(List.of(snapshot));

        var result = controller.getSnapshots(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo(10L);
        assertThat(result.get(0).outboundQueueDepth()).isEqualTo(5);
        assertThat(result.get(0).kafkaRequestsLag()).isEqualTo(10L);
    }
}
