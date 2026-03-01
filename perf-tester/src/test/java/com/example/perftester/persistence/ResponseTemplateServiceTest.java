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
class ResponseTemplateServiceTest {

    @Mock
    private ResponseTemplateRepository repository;

    @InjectMocks
    private ResponseTemplateService responseTemplateService;

    private ResponseTemplate templateWithId(Long id) {
        var template = new ResponseTemplate();
        template.setId(id);
        template.setName("template-" + id);
        template.setCreatedAt(Instant.now());
        template.setUpdatedAt(Instant.now());
        return template;
    }

    @Test
    void listAllShouldReturnSummaries() {
        when(repository.findAllByOrderByNameAsc()).thenReturn(List.of(templateWithId(1L)));

        var result = responseTemplateService.listAll();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo(1L);
        assertThat(result.get(0).name()).isEqualTo("template-1");
        assertThat(result.get(0).fieldCount()).isZero();
    }

    @Test
    void getByIdShouldReturnDetail() {
        when(repository.findById(1L)).thenReturn(Optional.of(templateWithId(1L)));

        var result = responseTemplateService.getById(1L);

        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.name()).isEqualTo("template-1");
        assertThat(result.fields()).isEmpty();
    }

    @Test
    void getByIdShouldThrowWhenNotFound() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> responseTemplateService.getById(99L))
                .isInstanceOf(ResponseTemplateNotFoundException.class);
    }

    @Test
    void createShouldSaveAndReturnDetail() {
        var request = new ResponseTemplateRequest("new-template", List.of());
        var saved = templateWithId(2L);
        saved.setName("new-template");
        when(repository.save(any())).thenReturn(saved);

        var result = responseTemplateService.create(request);

        verify(repository).save(any(ResponseTemplate.class));
        assertThat(result.name()).isEqualTo("new-template");
    }

    @Test
    void updateShouldModifyAndReturnDetail() {
        var template = templateWithId(1L);
        var request = new ResponseTemplateRequest("updated", List.of());
        var saved = templateWithId(1L);
        saved.setName("updated");
        when(repository.findById(1L)).thenReturn(Optional.of(template));
        when(repository.existsByNameAndIdNot("updated", 1L)).thenReturn(false);
        when(repository.save(template)).thenReturn(saved);

        var result = responseTemplateService.update(1L, request);

        assertThat(result.name()).isEqualTo("updated");
    }

    @Test
    void updateShouldThrowWhenNameConflicts() {
        var template = templateWithId(1L);
        var request = new ResponseTemplateRequest("taken", List.of());
        when(repository.findById(1L)).thenReturn(Optional.of(template));
        when(repository.existsByNameAndIdNot("taken", 1L)).thenReturn(true);

        assertThatThrownBy(() -> responseTemplateService.update(1L, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("taken");
    }

    @Test
    void updateShouldThrowWhenNotFound() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> responseTemplateService.update(99L, new ResponseTemplateRequest("name", List.of())))
                .isInstanceOf(ResponseTemplateNotFoundException.class);
    }

    @Test
    void deleteShouldCallDeleteById() {
        responseTemplateService.delete(1L);
        verify(repository).deleteById(1L);
    }
}
