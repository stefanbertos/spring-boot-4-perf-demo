package com.example.perftester.rest;

import com.example.perftester.persistence.InfraProfileService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InfraProfileControllerTest {

    @Mock
    private InfraProfileService infraProfileService;

    @InjectMocks
    private InfraProfileController controller;

    @Test
    void listAllShouldReturnOk() {
        var summary = new InfraProfileService.InfraProfileSummary(1L, "profile-a", "2024-01-01T00:00:00Z");
        when(infraProfileService.listAll()).thenReturn(List.of(summary));

        var response = controller.listAll();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
    }

    @Test
    void getByIdShouldReturnProfile() {
        var detail = new InfraProfileService.InfraProfileDetail(
                1L, "profile-a", Map.of(), Map.of(), Map.of(), Map.of(), "2024-01-01T00:00:00Z", "2024-01-01T00:00:00Z");
        when(infraProfileService.getById(1L)).thenReturn(detail);

        var response = controller.getById(1L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(detail);
    }

    @Test
    void createShouldReturnCreated() {
        var request = new InfraProfileService.InfraProfileRequest("profile-a", Map.of(), Map.of(), Map.of(), Map.of());
        var detail = new InfraProfileService.InfraProfileDetail(
                1L, "profile-a", Map.of(), Map.of(), Map.of(), Map.of(), "2024-01-01T00:00:00Z", "2024-01-01T00:00:00Z");
        when(infraProfileService.create(request)).thenReturn(detail);

        var response = controller.create(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isEqualTo(detail);
    }

    @Test
    void updateShouldReturnOk() {
        var request = new InfraProfileService.InfraProfileRequest("profile-a", Map.of(), Map.of(), Map.of(), Map.of());
        var detail = new InfraProfileService.InfraProfileDetail(
                1L, "profile-a", Map.of(), Map.of(), Map.of(), Map.of(), "2024-01-01T00:00:00Z", "2024-01-01T00:00:00Z");
        when(infraProfileService.update(1L, request)).thenReturn(detail);

        var response = controller.update(1L, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(detail);
    }

    @Test
    void deleteShouldReturnNoContent() {
        var response = controller.delete(1L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(infraProfileService).delete(1L);
    }

    @Test
    void applyShouldReturnApplyResult() {
        var result = new InfraProfileService.ApplyResult(List.of("log:com.example=DEBUG"), List.of());
        when(infraProfileService.applyProfile(1L)).thenReturn(result);

        var response = controller.apply(1L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().applied()).hasSize(1);
    }
}
