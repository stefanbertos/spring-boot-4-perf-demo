package com.example.perftester.rest;

import com.example.perftester.persistence.HeaderTemplateService;
import com.example.perftester.persistence.HeaderTemplateService.HeaderTemplateDetail;
import com.example.perftester.persistence.HeaderTemplateService.HeaderTemplateRequest;
import com.example.perftester.persistence.HeaderTemplateService.HeaderTemplateSummary;
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
@RequestMapping("/api/header-templates")
@RequiredArgsConstructor
public class HeaderTemplateController {

    private final HeaderTemplateService headerTemplateService;

    @GetMapping
    public ResponseEntity<List<HeaderTemplateSummary>> listAll() {
        return ResponseEntity.ok(headerTemplateService.listAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<HeaderTemplateDetail> getById(@PathVariable Long id) {
        return ResponseEntity.ok(headerTemplateService.getById(id));
    }

    @PostMapping
    public ResponseEntity<HeaderTemplateDetail> create(@RequestBody HeaderTemplateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(headerTemplateService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<HeaderTemplateDetail> update(@PathVariable Long id,
                                                       @RequestBody HeaderTemplateRequest request) {
        return ResponseEntity.ok(headerTemplateService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        headerTemplateService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
