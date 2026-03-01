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
class TestCaseServiceTest {

    @Mock
    private TestCaseRepository testCaseRepository;

    @Mock
    private HeaderTemplateRepository headerTemplateRepository;

    @Mock
    private ResponseTemplateRepository responseTemplateRepository;

    @InjectMocks
    private TestCaseService testCaseService;

    private TestCase caseWithId(long id) {
        var tc = new TestCase();
        tc.setId(id);
        tc.setName("case-" + id);
        tc.setMessage("msg-" + id);
        tc.setCreatedAt(Instant.now());
        tc.setUpdatedAt(Instant.now());
        return tc;
    }

    @Test
    void listAllShouldReturnDetails() {
        when(testCaseRepository.findAll()).thenReturn(List.of(caseWithId(1L)));

        var result = testCaseService.listAll();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo(1L);
        assertThat(result.get(0).name()).isEqualTo("case-1");
    }

    @Test
    void getByIdShouldReturnDetail() {
        when(testCaseRepository.findById(1L)).thenReturn(Optional.of(caseWithId(1L)));

        var result = testCaseService.getById(1L);

        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.message()).isEqualTo("msg-1");
    }

    @Test
    void getByIdShouldThrowWhenNotFound() {
        when(testCaseRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> testCaseService.getById(99L))
                .isInstanceOf(TestCaseNotFoundException.class);
    }

    @Test
    void createShouldSaveAndReturnDetail() {
        when(testCaseRepository.existsByName("new-case")).thenReturn(false);
        var saved = caseWithId(2L);
        saved.setName("new-case");
        saved.setMessage("hello");
        when(testCaseRepository.save(any())).thenReturn(saved);

        var result = testCaseService.create("new-case", "hello", null, null);

        assertThat(result.name()).isEqualTo("new-case");
        assertThat(result.message()).isEqualTo("hello");
        assertThat(result.headerTemplateId()).isNull();
        assertThat(result.responseTemplateId()).isNull();
    }

    @Test
    void createShouldThrowWhenNameConflicts() {
        when(testCaseRepository.existsByName("existing")).thenReturn(true);

        assertThatThrownBy(() -> testCaseService.create("existing", "msg", null, null))
                .isInstanceOf(TestCaseNameConflictException.class);
    }

    @Test
    void updateShouldModifyAndReturnDetail() {
        var tc = caseWithId(1L);
        when(testCaseRepository.findById(1L)).thenReturn(Optional.of(tc));
        when(testCaseRepository.existsByNameAndIdNot("updated-name", 1L)).thenReturn(false);
        var saved = caseWithId(1L);
        saved.setName("updated-name");
        when(testCaseRepository.save(tc)).thenReturn(saved);

        var result = testCaseService.update(1L, "updated-name", "new-msg", null, null);

        assertThat(result.name()).isEqualTo("updated-name");
    }

    @Test
    void updateShouldThrowWhenNotFound() {
        when(testCaseRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> testCaseService.update(99L, "name", "msg", null, null))
                .isInstanceOf(TestCaseNotFoundException.class);
    }

    @Test
    void updateShouldThrowWhenNameConflicts() {
        when(testCaseRepository.findById(1L)).thenReturn(Optional.of(caseWithId(1L)));
        when(testCaseRepository.existsByNameAndIdNot("taken", 1L)).thenReturn(true);

        assertThatThrownBy(() -> testCaseService.update(1L, "taken", "msg", null, null))
                .isInstanceOf(TestCaseNameConflictException.class);
    }

    @Test
    void deleteShouldCallDeleteById() {
        when(testCaseRepository.existsById(1L)).thenReturn(true);

        testCaseService.delete(1L);

        verify(testCaseRepository).deleteById(1L);
    }

    @Test
    void deleteShouldThrowWhenNotFound() {
        when(testCaseRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> testCaseService.delete(99L))
                .isInstanceOf(TestCaseNotFoundException.class);
    }

    @Test
    void createShouldApplyHeaderTemplateWhenProvided() {
        var headerTemplate = new HeaderTemplate();
        headerTemplate.setId(10L);
        headerTemplate.setName("header-A");
        when(testCaseRepository.existsByName("case")).thenReturn(false);
        when(headerTemplateRepository.findById(10L)).thenReturn(Optional.of(headerTemplate));
        var saved = caseWithId(3L);
        saved.setHeaderTemplate(headerTemplate);
        when(testCaseRepository.save(any())).thenReturn(saved);

        var result = testCaseService.create("case", "msg", 10L, null);

        assertThat(result.headerTemplateId()).isEqualTo(10L);
        assertThat(result.headerTemplateName()).isEqualTo("header-A");
    }
}
