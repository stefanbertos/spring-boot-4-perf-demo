package com.example.perftester.persistence;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TestScenarioServiceTest {

    @Mock
    private TestScenarioRepository testScenarioRepository;

    @Mock
    private TestCaseRepository testCaseRepository;

    @InjectMocks
    private TestScenarioService testScenarioService;

    private TestScenario scenarioWithId(Long id) {
        var scenario = new TestScenario();
        scenario.setId(id);
        scenario.setName("scenario-" + id);
        scenario.setCount(100);
        scenario.setCreatedAt(Instant.now());
        scenario.setUpdatedAt(Instant.now());
        return scenario;
    }

    private TestScenarioRequest emptyRequest(String name) {
        return new TestScenarioRequest(name, 100, List.of(), false, null, 0, null, null, null, List.of());
    }

    @Test
    void listAllShouldReturnSummaries() {
        var scenario = scenarioWithId(1L);
        when(testScenarioRepository.findAll()).thenReturn(List.of(scenario));

        var result = testScenarioService.listAll();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo(1L);
        assertThat(result.get(0).name()).isEqualTo("scenario-1");
        assertThat(result.get(0).count()).isEqualTo(100);
    }

    @Test
    void getByIdShouldReturnDetail() {
        var scenario = scenarioWithId(1L);
        when(testScenarioRepository.findById(1L)).thenReturn(Optional.of(scenario));

        var result = testScenarioService.getById(1L);

        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.name()).isEqualTo("scenario-1");
        assertThat(result.count()).isEqualTo(100);
    }

    @Test
    void getByIdShouldThrowWhenNotFound() {
        when(testScenarioRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> testScenarioService.getById(99L))
                .isInstanceOf(TestScenarioNotFoundException.class);
    }

    @Test
    void createShouldSaveAndReturnDetail() {
        var request = emptyRequest("new-scenario");
        var saved = scenarioWithId(2L);
        saved.setName("new-scenario");
        when(testScenarioRepository.save(any())).thenReturn(saved);

        var result = testScenarioService.create(request);

        verify(testScenarioRepository).save(any(TestScenario.class));
        assertThat(result.name()).isEqualTo("new-scenario");
    }

    @Test
    void updateShouldModifyAndReturnDetail() {
        var scenario = scenarioWithId(1L);
        var request = emptyRequest("updated-name");
        var saved = scenarioWithId(1L);
        saved.setName("updated-name");
        when(testScenarioRepository.findById(1L)).thenReturn(Optional.of(scenario));
        when(testScenarioRepository.save(scenario)).thenReturn(saved);

        var result = testScenarioService.update(1L, request);

        assertThat(result.name()).isEqualTo("updated-name");
    }

    @Test
    void updateShouldThrowWhenNotFound() {
        when(testScenarioRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> testScenarioService.update(99L, emptyRequest("name")))
                .isInstanceOf(TestScenarioNotFoundException.class);
    }

    @Test
    void deleteShouldCallDeleteById() {
        testScenarioService.delete(5L);
        verify(testScenarioRepository).deleteById(5L);
    }

    @Test
    void listScheduledEnabledShouldReturnOnlyEnabled() {
        var scenario = scenarioWithId(1L);
        scenario.setScheduledEnabled(true);
        scenario.setScheduledTime("08:00");
        when(testScenarioRepository.findByScheduledEnabledTrue()).thenReturn(List.of(scenario));

        var result = testScenarioService.listScheduledEnabled();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).scheduledEnabled()).isTrue();
    }

    @Test
    void getScenarioCountShouldReturnCount() {
        var scenario = scenarioWithId(1L);
        scenario.setCount(250);
        when(testScenarioRepository.findById(1L)).thenReturn(Optional.of(scenario));

        assertThat(testScenarioService.getScenarioCount(1L)).isEqualTo(250);
    }

    @Test
    void getScenarioCountShouldThrowWhenNotFound() {
        when(testScenarioRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> testScenarioService.getScenarioCount(99L))
                .isInstanceOf(TestScenarioNotFoundException.class);
    }

    @Test
    void getWarmupCountShouldReturnWarmupCount() {
        var scenario = scenarioWithId(1L);
        scenario.setWarmupCount(10);
        when(testScenarioRepository.findById(1L)).thenReturn(Optional.of(scenario));

        assertThat(testScenarioService.getWarmupCount(1L)).isEqualTo(10);
    }

    @Test
    void getScenarioThresholdsShouldReturnEmptyWhenJsonIsNull() {
        var scenario = scenarioWithId(1L);
        scenario.setThresholdsJson(null);
        when(testScenarioRepository.findById(1L)).thenReturn(Optional.of(scenario));

        assertThat(testScenarioService.getScenarioThresholds(1L)).isEmpty();
    }

    @Test
    void getScenarioThresholdsShouldReturnEmptyWhenJsonIsBlank() {
        var scenario = scenarioWithId(1L);
        scenario.setThresholdsJson("  ");
        when(testScenarioRepository.findById(1L)).thenReturn(Optional.of(scenario));

        assertThat(testScenarioService.getScenarioThresholds(1L)).isEmpty();
    }

    @Test
    void buildMessagePoolShouldReturnEmptyWhenNoEntries() {
        var scenario = scenarioWithId(1L);
        when(testScenarioRepository.findById(1L)).thenReturn(Optional.of(scenario));

        var pool = testScenarioService.buildMessagePool(1L);

        assertThat(pool).isEmpty();
    }
}
