package com.example.perftester.persistence;

import com.example.perftester.perf.PerfTestResult;
import com.example.perftester.perf.ThresholdResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TestRunServiceTest {

    @Mock
    private TestRunRepository testRunRepository;

    @Mock
    private TestRunSnapshotRepository snapshotRepository;

    @InjectMocks
    private TestRunService testRunService;

    private TestRun runWithId(Long id) {
        var run = new TestRun();
        run.setId(id);
        run.setTestRunId("uuid-" + id);
        run.setStatus("RUNNING");
        run.setStartedAt(Instant.now());
        return run;
    }

    private PerfTestResult emptyResult() {
        return new PerfTestResult(10, 0, 1.0, 10.0, 5.0, 1.0, 20.0);
    }

    @Test
    void createRunShouldSaveWithRunningStatus() {
        var saved = runWithId(1L);
        when(testRunRepository.save(any())).thenReturn(saved);

        var result = testRunService.createRun("uuid-1", "test-id", 100, "TYPE");

        var captor = ArgumentCaptor.forClass(TestRun.class);
        verify(testRunRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo("RUNNING");
        assertThat(captor.getValue().getMessageCount()).isEqualTo(100);
        assertThat(captor.getValue().getTestRunId()).isEqualTo("uuid-1");
        assertThat(result).isEqualTo(saved);
    }

    @Test
    void completeRunShouldUpdateStatusAndMetrics() {
        var run = runWithId(1L);
        when(testRunRepository.findById(1L)).thenReturn(Optional.of(run));

        testRunService.completeRun(1L, "COMPLETED", emptyResult(), "/tmp/export.zip");

        assertThat(run.getStatus()).isEqualTo("COMPLETED");
        assertThat(run.getCompletedCount()).isEqualTo(10L);
        assertThat(run.getTps()).isEqualTo(10.0);
        assertThat(run.getAvgLatencyMs()).isEqualTo(5.0);
        assertThat(run.getZipFilePath()).isEqualTo("/tmp/export.zip");
        assertThat(run.getCompletedAt()).isNotNull();
        verify(testRunRepository).save(run);
    }

    @Test
    void completeRunShouldThrowWhenNotFound() {
        when(testRunRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> testRunService.completeRun(99L, "COMPLETED", emptyResult(), null))
                .isInstanceOf(TestRunNotFoundException.class);
    }

    @Test
    void updateThresholdResultShouldSetStatusAndSerializeResults() {
        var run = runWithId(1L);
        when(testRunRepository.findById(1L)).thenReturn(Optional.of(run));
        var results = List.of(new ThresholdResult("TPS", "GTE", 100.0, 120.0, true));

        testRunService.updateThresholdResult(1L, "PASSED", results);

        assertThat(run.getThresholdStatus()).isEqualTo("PASSED");
        assertThat(run.getThresholdResults()).contains("TPS").contains("GTE");
        verify(testRunRepository).save(run);
    }

    @Test
    void updateThresholdResultShouldThrowWhenNotFound() {
        when(testRunRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> testRunService.updateThresholdResult(99L, "PASSED", List.of()))
                .isInstanceOf(TestRunNotFoundException.class);
    }

    @Test
    void findAllShouldDelegateToRepository() {
        var runs = List.of(runWithId(1L), runWithId(2L));
        when(testRunRepository.findAllByOrderByStartedAtDesc()).thenReturn(runs);

        assertThat(testRunService.findAll()).isEqualTo(runs);
    }

    @Test
    void findByIdShouldReturnRun() {
        var run = runWithId(5L);
        when(testRunRepository.findById(5L)).thenReturn(Optional.of(run));

        assertThat(testRunService.findById(5L)).isEqualTo(run);
    }

    @Test
    void findByIdShouldThrowWhenNotFound() {
        when(testRunRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> testRunService.findById(99L))
                .isInstanceOf(TestRunNotFoundException.class);
    }

    @Test
    void findSnapshotsShouldThrowWhenRunNotFound() {
        when(testRunRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> testRunService.findSnapshots(99L))
                .isInstanceOf(TestRunNotFoundException.class);
        verify(snapshotRepository, never()).findByTestRunIdOrderBySampledAtAsc(any());
    }

    @Test
    void findSnapshotsShouldReturnSnapshotsWhenRunExists() {
        when(testRunRepository.findById(1L)).thenReturn(Optional.of(runWithId(1L)));
        var snapshots = List.<TestRunSnapshot>of();
        when(snapshotRepository.findByTestRunIdOrderBySampledAtAsc(1L)).thenReturn(snapshots);

        assertThat(testRunService.findSnapshots(1L)).isEqualTo(snapshots);
    }

    @Test
    void deleteShouldDeleteRunWithoutZipFile() {
        when(testRunRepository.findById(1L)).thenReturn(Optional.of(runWithId(1L)));

        testRunService.delete(1L);

        verify(testRunRepository).deleteById(1L);
    }

    @Test
    void deleteShouldDeleteZipFileWhenPresent(@TempDir Path tempDir) throws IOException {
        var zipFile = Files.createTempFile(tempDir, "export", ".zip");
        var run = runWithId(1L);
        run.setZipFilePath(zipFile.toString());
        when(testRunRepository.findById(1L)).thenReturn(Optional.of(run));

        testRunService.delete(1L);

        assertThat(zipFile).doesNotExist();
        verify(testRunRepository).deleteById(1L);
    }

    @Test
    void deleteShouldThrowWhenNotFound() {
        when(testRunRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> testRunService.delete(99L))
                .isInstanceOf(TestRunNotFoundException.class);
    }

    @Test
    void findAllPagedShouldDelegateToRepository() {
        var run = runWithId(1L);
        var pageable = PageRequest.of(0, 20);
        when(testRunRepository.findAllByOrderByStartedAtDesc(pageable))
                .thenReturn(new PageImpl<>(List.of(run)));

        var result = testRunService.findAll(0, 20, null);

        assertThat(result.getContent()).containsExactly(run);
        assertThat(result.getTotalElements()).isEqualTo(1L);
    }

    @Test
    void findAllPagedWithTagShouldFilterByTag() {
        var run = runWithId(1L);
        var pageable = PageRequest.of(0, 20);
        when(testRunRepository.findByTagsLike("%smoke%", pageable))
                .thenReturn(new PageImpl<>(List.of(run)));

        var result = testRunService.findAll(0, 20, "smoke");

        assertThat(result.getContent()).containsExactly(run);
    }

    @Test
    void setTagsShouldSerializeAndSave() {
        var run = runWithId(1L);
        when(testRunRepository.findById(1L)).thenReturn(Optional.of(run));

        testRunService.setTags(1L, List.of("smoke", "before-deploy"));

        assertThat(run.getTags()).contains("smoke").contains("before-deploy");
        verify(testRunRepository).save(run);
    }

    @Test
    void getAllUniqueTagsShouldDeduplicateAndSort() {
        when(testRunRepository.findAllTagsJson())
                .thenReturn(List.of("[\"b\",\"a\"]", "[\"a\",\"c\"]"));

        var tags = testRunService.getAllUniqueTags();

        assertThat(tags).containsExactly("a", "b", "c");
    }

    @Test
    void getAllUniqueTagsShouldReturnEmptyWhenNoTags() {
        when(testRunRepository.findAllTagsJson()).thenReturn(List.of());

        assertThat(testRunService.getAllUniqueTags()).isEmpty();
    }

    @Test
    void bulkDeleteShouldDeleteAllIds() {
        var run1 = runWithId(1L);
        var run2 = runWithId(2L);
        when(testRunRepository.findById(1L)).thenReturn(Optional.of(run1));
        when(testRunRepository.findById(2L)).thenReturn(Optional.of(run2));

        testRunService.bulkDelete(List.of(1L, 2L));

        verify(testRunRepository).deleteById(1L);
        verify(testRunRepository).deleteById(2L);
        verify(testRunRepository, times(2)).deleteById(any());
    }

    @Test
    void getTrendDataShouldDelegateToRepository() {
        var runs = List.of(runWithId(1L), runWithId(2L));
        when(testRunRepository.findCompletedOrderByStartedAtAsc()).thenReturn(runs);

        assertThat(testRunService.getTrendData()).isEqualTo(runs);
    }
}
