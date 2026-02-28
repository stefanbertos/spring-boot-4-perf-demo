package com.example.perftester.preferences;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Run Test Preferences", description = "Persist and retrieve user preferences for the run test form")
@RestController
@RequestMapping("/api/run-test/preferences")
@RequiredArgsConstructor
public class RunTestPreferencesController {

    private final RunTestPreferencesService service;

    @Operation(summary = "Get run test preferences")
    @GetMapping
    public RunTestPreferencesResponse get() {
        return service.get();
    }

    @Operation(summary = "Update run test preferences")
    @PutMapping
    public RunTestPreferencesResponse update(@RequestBody RunTestPreferencesRequest request) {
        return service.update(request);
    }
}
