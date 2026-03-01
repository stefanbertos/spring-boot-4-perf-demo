package com.example.perftester.rest;

import com.example.perftester.persistence.TestCaseDetail;
import com.example.perftester.persistence.TestCaseService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TestCaseControllerTest {

    @Mock
    private TestCaseService testCaseService;

    @InjectMocks
    private TestCaseController controller;

    private TestCaseDetail detailWithId(long id) {
        return new TestCaseDetail(id, "case-" + id, "msg-" + id,
                null, null, null, null,
                Instant.now(), Instant.now());
    }

    @Test
    void listAllShouldReturnSummaries() {
        when(testCaseService.listAll()).thenReturn(List.of(detailWithId(1L), detailWithId(2L)));

        var result = controller.listAll();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).id()).isEqualTo(1L);
        assertThat(result.get(0).name()).isEqualTo("case-1");
    }

    @Test
    void getByIdShouldReturnResponse() {
        when(testCaseService.getById(1L)).thenReturn(detailWithId(1L));

        var result = controller.getById(1L);

        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.message()).isEqualTo("msg-1");
    }

    @Test
    void createShouldReturnCreatedTestCase() {
        var request = new CreateTestCaseRequest("new-case", "hello world", null, null);
        var detail = detailWithId(3L);
        when(testCaseService.create("new-case", "hello world", null, null)).thenReturn(detail);

        var result = controller.create(request);

        assertThat(result.id()).isEqualTo(3L);
    }

    @Test
    void updateShouldReturnUpdatedTestCase() {
        var request = new UpdateTestCaseRequest("updated-name", "new message", null, null);
        var detail = new TestCaseDetail(1L, "updated-name", "new message",
                null, null, null, null, Instant.now(), Instant.now());
        when(testCaseService.update(1L, "updated-name", "new message", null, null)).thenReturn(detail);

        var result = controller.update(1L, request);

        assertThat(result.name()).isEqualTo("updated-name");
        assertThat(result.message()).isEqualTo("new message");
    }

    @Test
    void deleteShouldDelegateToService() {
        controller.delete(1L);
        verify(testCaseService).delete(1L);
    }

    @Test
    void uploadShouldCreateTestCaseFromFileContent() throws IOException {
        var content = "message from file";
        var file = new MockMultipartFile("file", "test.txt", "text/plain",
                content.getBytes(StandardCharsets.UTF_8));
        var detail = new TestCaseDetail(5L, "uploaded", content,
                null, null, null, null, Instant.now(), Instant.now());
        when(testCaseService.create("uploaded", content, null, null)).thenReturn(detail);

        var result = controller.upload("uploaded", file);

        assertThat(result.id()).isEqualTo(5L);
        assertThat(result.message()).isEqualTo(content);
    }
}
