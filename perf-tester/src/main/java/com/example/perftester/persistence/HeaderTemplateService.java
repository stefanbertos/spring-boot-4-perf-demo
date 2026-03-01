package com.example.perftester.persistence;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class HeaderTemplateService {

    private final HeaderTemplateRepository repository;

    @Transactional(readOnly = true)
    public List<HeaderTemplateSummary> listAll() {
        return repository.findAllByOrderByNameAsc().stream()
                .map(t -> new HeaderTemplateSummary(t.getId(), t.getName(),
                        t.getFields() == null ? 0 : t.getFields().size(),
                        t.getUpdatedAt().toString()))
                .toList();
    }

    @Transactional(readOnly = true)
    public HeaderTemplateDetail getById(Long id) {
        return toDetail(findOrThrow(id));
    }

    @Transactional
    public HeaderTemplateDetail create(HeaderTemplateRequest request) {
        var template = new HeaderTemplate();
        template.setName(request.name());
        template.setFields(toFields(request.fields()));
        return toDetail(repository.save(template));
    }

    @Transactional
    public HeaderTemplateDetail update(Long id, HeaderTemplateRequest request) {
        var template = findOrThrow(id);
        if (repository.existsByNameAndIdNot(request.name(), id)) {
            throw new IllegalArgumentException("A header template named '" + request.name() + "' already exists");
        }
        template.setName(request.name());
        template.setFields(toFields(request.fields()));
        return toDetail(repository.save(template));
    }

    @Transactional
    public void delete(Long id) {
        repository.deleteById(id);
    }

    private HeaderTemplate findOrThrow(Long id) {
        return repository.findById(id).orElseThrow(() -> new HeaderTemplateNotFoundException(id));
    }

    private HeaderTemplateDetail toDetail(HeaderTemplate t) {
        var fieldDtos = t.getFields() == null ? List.<TemplateFieldDto>of()
                : t.getFields().stream()
                        .map(f -> new TemplateFieldDto(f.name(), f.size(), f.value(),
                                f.type(), f.paddingChar(), f.uuidPrefix(), f.uuidSeparator(),
                                f.correlationKey()))
                        .toList();
        return new HeaderTemplateDetail(t.getId(), t.getName(), fieldDtos,
                t.getCreatedAt().toString(), t.getUpdatedAt().toString());
    }

    private List<HeaderTemplate.TemplateField> toFields(List<TemplateFieldDto> dtos) {
        if (dtos == null) {
            return new ArrayList<>();
        }
        return dtos.stream()
                .map(d -> new HeaderTemplate.TemplateField(d.name(), d.size(), d.value(),
                        d.type(), d.paddingChar(), d.uuidPrefix(), d.uuidSeparator(),
                        d.correlationKey()))
                .toList();
    }

}
