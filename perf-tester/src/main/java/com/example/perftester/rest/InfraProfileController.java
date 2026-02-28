package com.example.perftester.rest;

import java.util.List;

import com.example.perftester.persistence.InfraProfileService;
import com.example.perftester.persistence.InfraProfileService.ApplyResult;
import com.example.perftester.persistence.InfraProfileService.InfraProfileDetail;
import com.example.perftester.persistence.InfraProfileService.InfraProfileRequest;
import com.example.perftester.persistence.InfraProfileService.InfraProfileSummary;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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

@Tag(name = "Infrastructure Profiles", description = "Manage and apply infrastructure profiles that preset Kafka partitions and MQ queue depths for test scenarios")
@Slf4j
@Validated
@RestController
@RequestMapping("/api/infra-profiles")
@RequiredArgsConstructor
public class InfraProfileController {

    private final InfraProfileService infraProfileService;

    @Operation(summary = "List all infrastructure profiles")
    @GetMapping
    public ResponseEntity<List<InfraProfileSummary>> listAll() {
        return ResponseEntity.ok(infraProfileService.listAll());
    }

    @Operation(summary = "Get an infrastructure profile by ID")
    @GetMapping("/{id}")
    public ResponseEntity<InfraProfileDetail> getById(@PathVariable Long id) {
        return ResponseEntity.ok(infraProfileService.getById(id));
    }

    @Operation(summary = "Create an infrastructure profile")
    @PostMapping
    public ResponseEntity<InfraProfileDetail> create(@Valid @RequestBody InfraProfileRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(infraProfileService.create(request));
    }

    @Operation(summary = "Update an infrastructure profile")
    @PutMapping("/{id}")
    public ResponseEntity<InfraProfileDetail> update(
            @PathVariable Long id,
            @Valid @RequestBody InfraProfileRequest request) {
        return ResponseEntity.ok(infraProfileService.update(id, request));
    }

    @Operation(summary = "Delete an infrastructure profile")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        infraProfileService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Apply an infrastructure profile", description = "Applies the profile settings: sets Kafka partitions and MQ queue depths as configured")
    @PostMapping("/{id}/apply")
    public ResponseEntity<ApplyResult> apply(@PathVariable Long id) {
        return ResponseEntity.ok(infraProfileService.applyProfile(id));
    }
}
