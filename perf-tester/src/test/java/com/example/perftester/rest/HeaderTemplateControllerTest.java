package com.example.perftester.rest;

import com.example.perftester.persistence.HeaderTemplateService;
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
class HeaderTemplateControllerTest {

    @Mock
    private HeaderTemplateService headerTemplateService;

    @InjectMocks
    private HeaderTemplateController controller;

    @Test
    void listAllShouldReturnOk() {
        var summary = new HeaderTemplateService.HeaderTemplateSummary(1L, "Correlation", 2, "2024-01-01T00:00:00Z");
        when(headerTemplateService.listAll()).thenReturn(List.of(summary));

        var response = controller.listAll();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
    }

    @Test
    void getByIdShouldReturnDetail() {
        var field = new HeaderTemplateService.TemplateFieldDto("CorrelationId", 32, "ABC", null, null, null, null);
        var detail = new HeaderTemplateService.HeaderTemplateDetail(
                1L, "Correlation", List.of(field), "2024-01-01T00:00:00Z", "2024-01-01T00:00:00Z");
        when(headerTemplateService.getById(1L)).thenReturn(detail);

        var response = controller.getById(1L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(detail);
    }

    @Test
    void createShouldReturnCreated() {
        var field = new HeaderTemplateService.TemplateFieldDto("MsgType", 10, "REQUEST", null, null, null, null);
        var request = new HeaderTemplateService.HeaderTemplateRequest("New Template", List.of(field));
        var detail = new HeaderTemplateService.HeaderTemplateDetail(
                2L, "New Template", List.of(field), "2024-01-01T00:00:00Z", "2024-01-01T00:00:00Z");
        when(headerTemplateService.create(request)).thenReturn(detail);

        var response = controller.create(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isEqualTo(detail);
    }

    @Test
    void updateShouldReturnOk() {
        var field = new HeaderTemplateService.TemplateFieldDto("System", 8, "PERF", null, null, null, null);
        var request = new HeaderTemplateService.HeaderTemplateRequest("Updated Template", List.of(field));
        var detail = new HeaderTemplateService.HeaderTemplateDetail(
                1L, "Updated Template", List.of(field), "2024-01-01T00:00:00Z", "2024-01-02T00:00:00Z");
        when(headerTemplateService.update(1L, request)).thenReturn(detail);

        var response = controller.update(1L, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(detail);
    }

    @Test
    void deleteShouldReturnNoContent() {
        var response = controller.delete(1L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(headerTemplateService).delete(1L);
    }
}
