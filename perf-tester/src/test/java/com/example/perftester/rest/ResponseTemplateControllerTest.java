package com.example.perftester.rest;

import com.example.perftester.persistence.ResponseFieldDto;
import com.example.perftester.persistence.ResponseTemplateDetail;
import com.example.perftester.persistence.ResponseTemplateRequest;
import com.example.perftester.persistence.ResponseTemplateService;
import com.example.perftester.persistence.ResponseTemplateSummary;
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
class ResponseTemplateControllerTest {

    @Mock
    private ResponseTemplateService responseTemplateService;

    @InjectMocks
    private ResponseTemplateController controller;

    @Test
    void listAllShouldReturnOk() {
        var summary = new ResponseTemplateSummary(1L, "Standard Response", 3, "2024-01-01T00:00:00Z");
        when(responseTemplateService.listAll()).thenReturn(List.of(summary));

        var response = controller.listAll();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
    }

    @Test
    void getByIdShouldReturnDetail() {
        var field = new ResponseFieldDto("Status", 8, "PROCESSED", "STATIC", " ");
        var detail = new ResponseTemplateDetail(
                1L, "Standard Response", List.of(field), "2024-01-01T00:00:00Z", "2024-01-01T00:00:00Z");
        when(responseTemplateService.getById(1L)).thenReturn(detail);

        var response = controller.getById(1L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(detail);
    }

    @Test
    void createShouldReturnCreated() {
        var field = new ResponseFieldDto("CorrelationId", 32, null, "IGNORE", null);
        var request = new ResponseTemplateRequest("New Response", List.of(field));
        var detail = new ResponseTemplateDetail(
                2L, "New Response", List.of(field), "2024-01-01T00:00:00Z", "2024-01-01T00:00:00Z");
        when(responseTemplateService.create(request)).thenReturn(detail);

        var response = controller.create(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isEqualTo(detail);
    }

    @Test
    void updateShouldReturnOk() {
        var field = new ResponseFieldDto("MsgType", 10, "RESPONSE", "STATIC", " ");
        var request = new ResponseTemplateRequest("Updated Response", List.of(field));
        var detail = new ResponseTemplateDetail(
                1L, "Updated Response", List.of(field), "2024-01-01T00:00:00Z", "2024-01-02T00:00:00Z");
        when(responseTemplateService.update(1L, request)).thenReturn(detail);

        var response = controller.update(1L, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(detail);
    }

    @Test
    void deleteShouldReturnNoContent() {
        var response = controller.delete(1L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(responseTemplateService).delete(1L);
    }
}
