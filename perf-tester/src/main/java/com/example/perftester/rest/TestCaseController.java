package com.example.perftester.rest;

import com.example.perftester.persistence.TestCaseDetail;
import com.example.perftester.persistence.TestCaseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Tag(name = "Test Cases", description = "Manage test case messages used as payloads in performance tests")
@RestController
@RequestMapping("/api/test-cases")
@RequiredArgsConstructor
public class TestCaseController {

    private final TestCaseService testCaseService;

    @Operation(summary = "List all test cases")
    @GetMapping
    public List<TestCaseSummaryResponse> listAll() {
        return testCaseService.listAll().stream()
                .map(tc -> new TestCaseSummaryResponse(tc.id(), tc.name(),
                        tc.headerTemplateId(), tc.headerTemplateName(),
                        tc.responseTemplateId(), tc.responseTemplateName(),
                        tc.updatedAt()))
                .toList();
    }

    @Operation(summary = "Get a test case by ID")
    @GetMapping("/{id}")
    public TestCaseResponse getById(@PathVariable long id) {
        return toResponse(testCaseService.getById(id));
    }

    @Operation(summary = "Create a test case")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TestCaseResponse create(@Valid @RequestBody CreateTestCaseRequest request) {
        return toResponse(testCaseService.create(request.name(), request.message(),
                request.headerTemplateId(), request.responseTemplateId()));
    }

    @Operation(summary = "Update a test case")
    @PutMapping("/{id}")
    public TestCaseResponse update(@PathVariable long id,
                                   @Valid @RequestBody UpdateTestCaseRequest request) {
        return toResponse(testCaseService.update(id, request.name(), request.message(),
                request.headerTemplateId(), request.responseTemplateId()));
    }

    @Operation(summary = "Delete a test case")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable long id) {
        testCaseService.delete(id);
    }

    @Operation(summary = "Upload a test case from a file", description = "Creates a test case by reading the message content from an uploaded file")
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public TestCaseResponse upload(@RequestParam String name,
                                   @RequestParam MultipartFile file) throws IOException {
        var message = new String(file.getBytes(), StandardCharsets.UTF_8);
        return toResponse(testCaseService.create(name, message, null, null));
    }

    private TestCaseResponse toResponse(TestCaseDetail tc) {
        return new TestCaseResponse(tc.id(), tc.name(), tc.message(),
                tc.headerTemplateId(), tc.headerTemplateName(),
                tc.responseTemplateId(), tc.responseTemplateName(),
                tc.createdAt(), tc.updatedAt());
    }
}
