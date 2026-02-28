package com.example.perftester.rest;

import com.example.perftester.loki.LokiServiceLabelService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Admin: Loki", description = "Manage the list of service labels used when querying logs from Loki")
@RestController
@RequestMapping("/api/admin/loki/services")
@RequiredArgsConstructor
@Validated
public class LokiAdminController {

    private final LokiServiceLabelService lokiServiceLabelService;

    @Operation(summary = "List Loki service labels")
    @GetMapping
    public List<String> list() {
        return lokiServiceLabelService.findAll();
    }

    @Operation(summary = "Add a Loki service label")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public String add(@NotBlank @RequestParam String name) {
        return lokiServiceLabelService.add(name.trim());
    }

    @Operation(summary = "Remove a Loki service label")
    @DeleteMapping("/{name}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String name) {
        lokiServiceLabelService.delete(name);
    }
}
