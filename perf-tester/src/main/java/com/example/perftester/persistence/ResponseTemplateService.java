package com.example.perftester.persistence;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ResponseTemplateService {

    private final ResponseTemplateRepository repository;

    @Transactional(readOnly = true)
    public List<ResponseTemplateSummary> listAll() {
        return repository.findAllByOrderByNameAsc().stream()
                .map(t -> new ResponseTemplateSummary(t.getId(), t.getName(),
                        t.getFields() == null ? 0 : t.getFields().size(),
                        t.getUpdatedAt().toString()))
                .toList();
    }

    @Transactional(readOnly = true)
    public ResponseTemplateDetail getById(Long id) {
        return toDetail(findOrThrow(id));
    }

    @Transactional
    public ResponseTemplateDetail create(ResponseTemplateRequest request) {
        var template = new ResponseTemplate();
        template.setName(request.name());
        template.setFields(toFields(request.fields()));
        return toDetail(repository.save(template));
    }

    @Transactional
    public ResponseTemplateDetail update(Long id, ResponseTemplateRequest request) {
        var template = findOrThrow(id);
        if (repository.existsByNameAndIdNot(request.name(), id)) {
            throw new IllegalArgumentException("A response template named '" + request.name() + "' already exists");
        }
        template.setName(request.name());
        template.setFields(toFields(request.fields()));
        return toDetail(repository.save(template));
    }

    @Transactional
    public void delete(Long id) {
        repository.deleteById(id);
    }

    private ResponseTemplate findOrThrow(Long id) {
        return repository.findById(id).orElseThrow(() -> new ResponseTemplateNotFoundException(id));
    }

    private ResponseTemplateDetail toDetail(ResponseTemplate t) {
        var fieldDtos = t.getFields() == null ? List.<ResponseFieldDto>of()
                : t.getFields().stream()
                        .map(f -> new ResponseFieldDto(f.name(), f.size(), f.value(), f.type(), f.paddingChar()))
                        .toList();
        return new ResponseTemplateDetail(t.getId(), t.getName(), fieldDtos,
                t.getCreatedAt().toString(), t.getUpdatedAt().toString());
    }

    private List<ResponseTemplate.ResponseField> toFields(List<ResponseFieldDto> dtos) {
        if (dtos == null) {
            return new ArrayList<>();
        }
        return dtos.stream()
                .map(d -> new ResponseTemplate.ResponseField(d.name(), d.size(), d.value(), d.type(), d.paddingChar()))
                .toList();
    }

    public record ResponseFieldDto(String name, int size, String value, String type, String paddingChar) {
    }

    public record ResponseTemplateSummary(Long id, String name, int fieldCount, String updatedAt) {
    }

    public record ResponseTemplateDetail(Long id, String name, List<ResponseFieldDto> fields,
                                         String createdAt, String updatedAt) {
    }

    public record ResponseTemplateRequest(String name, List<ResponseFieldDto> fields) {
    }
}
