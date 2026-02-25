package com.example.perftester.rest;

import com.example.perftester.loki.LokiServiceLabelService;
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

@RestController
@RequestMapping("/api/admin/loki/services")
@RequiredArgsConstructor
@Validated
public class LokiAdminController {

    private final LokiServiceLabelService lokiServiceLabelService;

    @GetMapping
    public List<String> list() {
        return lokiServiceLabelService.findAll();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public String add(@NotBlank @RequestParam String name) {
        return lokiServiceLabelService.add(name.trim());
    }

    @DeleteMapping("/{name}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String name) {
        lokiServiceLabelService.delete(name);
    }
}
