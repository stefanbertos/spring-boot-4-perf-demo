package com.example.perftester.rest;

import com.example.perftester.persistence.TestScenarioService;
import com.example.perftester.persistence.TestScenarioService.TestScenarioDetail;
import com.example.perftester.persistence.TestScenarioService.TestScenarioRequest;
import com.example.perftester.persistence.TestScenarioService.TestScenarioSummary;
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
@RequestMapping("/api/test-scenarios")
@RequiredArgsConstructor
public class TestScenarioController {

    private final TestScenarioService testScenarioService;

    @GetMapping
    public ResponseEntity<List<TestScenarioSummary>> listAll() {
        return ResponseEntity.ok(testScenarioService.listAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<TestScenarioDetail> getById(@PathVariable Long id) {
        return ResponseEntity.ok(testScenarioService.getById(id));
    }

    @PostMapping
    public ResponseEntity<TestScenarioDetail> create(@RequestBody TestScenarioRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(testScenarioService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<TestScenarioDetail> update(@PathVariable Long id,
                                                     @RequestBody TestScenarioRequest request) {
        return ResponseEntity.ok(testScenarioService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        testScenarioService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
