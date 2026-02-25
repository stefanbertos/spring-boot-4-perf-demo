package com.example.perftester.loki;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LokiServiceLabelService {

    private final LokiServiceLabelRepository repository;

    @Transactional(readOnly = true)
    public List<String> findAll() {
        return repository.findAll().stream()
                .map(LokiServiceLabel::getName)
                .sorted()
                .toList();
    }

    @Transactional
    public String add(String name) {
        if (repository.existsById(name)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Service label already exists: " + name);
        }
        repository.save(new LokiServiceLabel(name));
        return name;
    }

    @Transactional
    public void delete(String name) {
        if (!repository.existsById(name)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Service label not found: " + name);
        }
        repository.deleteById(name);
    }
}
