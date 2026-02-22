package com.example.perftester.rest;

import java.util.List;

import com.example.perftester.persistence.InfraProfileService;
import com.example.perftester.persistence.InfraProfileService.ApplyResult;
import com.example.perftester.persistence.InfraProfileService.InfraProfileDetail;
import com.example.perftester.persistence.InfraProfileService.InfraProfileRequest;
import com.example.perftester.persistence.InfraProfileService.InfraProfileSummary;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@Validated
@RestController
@RequestMapping("/api/infra-profiles")
@RequiredArgsConstructor
public class InfraProfileController {

    private final InfraProfileService infraProfileService;

    @GetMapping
    public ResponseEntity<List<InfraProfileSummary>> listAll() {
        return ResponseEntity.ok(infraProfileService.listAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<InfraProfileDetail> getById(@PathVariable Long id) {
        return ResponseEntity.ok(infraProfileService.getById(id));
    }

    @PostMapping
    public ResponseEntity<InfraProfileDetail> create(@Valid @RequestBody InfraProfileRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(infraProfileService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<InfraProfileDetail> update(
            @PathVariable Long id,
            @Valid @RequestBody InfraProfileRequest request) {
        return ResponseEntity.ok(infraProfileService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        infraProfileService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/apply")
    public ResponseEntity<ApplyResult> apply(@PathVariable Long id) {
        return ResponseEntity.ok(infraProfileService.applyProfile(id));
    }
}
