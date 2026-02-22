package com.example.perftester.rest;

import com.example.perftester.persistence.ResponseTemplateService;
import com.example.perftester.persistence.ResponseTemplateService.ResponseTemplateDetail;
import com.example.perftester.persistence.ResponseTemplateService.ResponseTemplateRequest;
import com.example.perftester.persistence.ResponseTemplateService.ResponseTemplateSummary;
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

@RestController
@RequestMapping("/api/response-templates")
@RequiredArgsConstructor
public class ResponseTemplateController {

    private final ResponseTemplateService responseTemplateService;

    @GetMapping
    public ResponseEntity<List<ResponseTemplateSummary>> listAll() {
        return ResponseEntity.ok(responseTemplateService.listAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ResponseTemplateDetail> getById(@PathVariable Long id) {
        return ResponseEntity.ok(responseTemplateService.getById(id));
    }

    @PostMapping
    public ResponseEntity<ResponseTemplateDetail> create(@RequestBody ResponseTemplateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(responseTemplateService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ResponseTemplateDetail> update(@PathVariable Long id,
                                                         @RequestBody ResponseTemplateRequest request) {
        return ResponseEntity.ok(responseTemplateService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        responseTemplateService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
