package com.example.perftester.rest;

import com.example.perftester.persistence.ResponseTemplateService;
import com.example.perftester.persistence.ResponseTemplateService.ResponseTemplateDetail;
import com.example.perftester.persistence.ResponseTemplateService.ResponseTemplateRequest;
import com.example.perftester.persistence.ResponseTemplateService.ResponseTemplateSummary;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Response Templates", description = "Manage response validation templates that define how inbound MQ response fields are matched")
@RestController
@RequestMapping("/api/response-templates")
@RequiredArgsConstructor
public class ResponseTemplateController {

    private final ResponseTemplateService responseTemplateService;

    @Operation(summary = "List all response templates")
    @GetMapping
    public ResponseEntity<List<ResponseTemplateSummary>> listAll() {
        return ResponseEntity.ok(responseTemplateService.listAll());
    }

    @Operation(summary = "Get a response template by ID")
    @GetMapping("/{id}")
    public ResponseEntity<ResponseTemplateDetail> getById(@PathVariable Long id) {
        return ResponseEntity.ok(responseTemplateService.getById(id));
    }

    @Operation(summary = "Create a response template")
    @PostMapping
    public ResponseEntity<ResponseTemplateDetail> create(@RequestBody ResponseTemplateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(responseTemplateService.create(request));
    }

    @Operation(summary = "Update a response template")
    @PutMapping("/{id}")
    public ResponseEntity<ResponseTemplateDetail> update(@PathVariable Long id,
                                                         @RequestBody ResponseTemplateRequest request) {
        return ResponseEntity.ok(responseTemplateService.update(id, request));
    }

    @Operation(summary = "Delete a response template")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        responseTemplateService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
