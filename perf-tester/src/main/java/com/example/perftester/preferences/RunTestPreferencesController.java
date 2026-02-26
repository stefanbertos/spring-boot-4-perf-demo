package com.example.perftester.preferences;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/run-test/preferences")
@RequiredArgsConstructor
public class RunTestPreferencesController {

    private final RunTestPreferencesService service;

    @GetMapping
    public RunTestPreferencesResponse get() {
        return service.get();
    }

    @PutMapping
    public RunTestPreferencesResponse update(@RequestBody RunTestPreferencesRequest request) {
        return service.update(request);
    }
}
