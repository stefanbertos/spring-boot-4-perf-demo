package com.example.perftester.rest;

import com.example.perftester.persistence.TestScenarioService;
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
class TestScenarioControllerTest {

    @Mock
    private TestScenarioService testScenarioService;

    @InjectMocks
    private TestScenarioController controller;

    @Test
    void listAllShouldReturnOk() {
        var summary = new TestScenarioService.TestScenarioSummary(1L, "scenario-a", 100, "2024-01-01T00:00:00Z");
        when(testScenarioService.listAll()).thenReturn(List.of(summary));

        var response = controller.listAll();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
    }

    @Test
    void getByIdShouldReturnScenario() {
        var detail = new TestScenarioService.TestScenarioDetail(
                1L, "scenario-a", 100, List.of(), false, null, 0, null, null, List.of(), "2024-01-01T00:00:00Z", "2024-01-01T00:00:00Z");
        when(testScenarioService.getById(1L)).thenReturn(detail);

        var response = controller.getById(1L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(detail);
    }

    @Test
    void createShouldReturnCreated() {
        var request = new TestScenarioService.TestScenarioRequest("scenario-a", 100, List.of(), false, null, 0, null, null, List.of());
        var detail = new TestScenarioService.TestScenarioDetail(
                1L, "scenario-a", 100, List.of(), false, null, 0, null, null, List.of(), "2024-01-01T00:00:00Z", "2024-01-01T00:00:00Z");
        when(testScenarioService.create(request)).thenReturn(detail);

        var response = controller.create(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isEqualTo(detail);
    }

    @Test
    void updateShouldReturnOk() {
        var request = new TestScenarioService.TestScenarioRequest("scenario-a", 100, List.of(), false, null, 0, null, null, List.of());
        var detail = new TestScenarioService.TestScenarioDetail(
                1L, "scenario-a", 100, List.of(), false, null, 0, null, null, List.of(), "2024-01-01T00:00:00Z", "2024-01-01T00:00:00Z");
        when(testScenarioService.update(1L, request)).thenReturn(detail);

        var response = controller.update(1L, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(detail);
    }

    @Test
    void deleteShouldReturnNoContent() {
        var response = controller.delete(1L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(testScenarioService).delete(1L);
    }
}
