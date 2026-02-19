package com.example.perftester.rest;

import com.example.perftester.persistence.TestCase;
import com.example.perftester.persistence.TestCaseService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
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
import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/test-cases")
@RequiredArgsConstructor
public class TestCaseController {

    private final TestCaseService testCaseService;

    public record CreateTestCaseRequest(@NotBlank String name, @NotBlank String message) { }

    public record UpdateTestCaseRequest(@NotBlank String name, @NotBlank String message) { }

    public record TestCaseResponse(long id, String name, String message,
                                   Instant createdAt, Instant updatedAt) { }

    public record TestCaseSummaryResponse(long id, String name, Instant updatedAt) { }

    @GetMapping
    public List<TestCaseSummaryResponse> listAll() {
        return testCaseService.listAll().stream()
                .map(tc -> new TestCaseSummaryResponse(tc.getId(), tc.getName(), tc.getUpdatedAt()))
                .toList();
    }

    @GetMapping("/{id}")
    public TestCaseResponse getById(@PathVariable long id) {
        var tc = testCaseService.getById(id);
        return toResponse(tc);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TestCaseResponse create(@Valid @RequestBody CreateTestCaseRequest request) {
        var tc = testCaseService.create(request.name(), request.message());
        return toResponse(tc);
    }

    @PutMapping("/{id}")
    public TestCaseResponse update(@PathVariable long id,
                                   @Valid @RequestBody UpdateTestCaseRequest request) {
        var tc = testCaseService.update(id, request.name(), request.message());
        return toResponse(tc);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable long id) {
        testCaseService.delete(id);
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public TestCaseResponse upload(@RequestParam String name,
                                   @RequestParam MultipartFile file) throws IOException {
        var message = new String(file.getBytes(), StandardCharsets.UTF_8);
        var tc = testCaseService.create(name, message);
        return toResponse(tc);
    }

    private TestCaseResponse toResponse(TestCase tc) {
        return new TestCaseResponse(tc.getId(), tc.getName(), tc.getMessage(),
                tc.getCreatedAt(), tc.getUpdatedAt());
    }
}
