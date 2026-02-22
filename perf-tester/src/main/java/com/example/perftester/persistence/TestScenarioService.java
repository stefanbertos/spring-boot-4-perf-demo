package com.example.perftester.persistence;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TestScenarioService {

    private final TestScenarioRepository testScenarioRepository;

    @Transactional(readOnly = true)
    public List<TestScenarioSummary> listAll() {
        return testScenarioRepository.findAll().stream()
                .map(s -> new TestScenarioSummary(s.getId(), s.getName(), s.getCount(),
                        s.getUpdatedAt().toString()))
                .toList();
    }

    @Transactional(readOnly = true)
    public TestScenarioDetail getById(Long id) {
        var scenario = testScenarioRepository.findById(id)
                .orElseThrow(() -> new TestScenarioNotFoundException(id));
        return toDetail(scenario);
    }

    @Transactional
    public TestScenarioDetail create(TestScenarioRequest request) {
        var scenario = new TestScenario();
        scenario.setName(request.name());
        scenario.setCount(request.count());
        scenario.setEntries(toEntries(request.entries()));
        return toDetail(testScenarioRepository.save(scenario));
    }

    @Transactional
    public TestScenarioDetail update(Long id, TestScenarioRequest request) {
        var scenario = testScenarioRepository.findById(id)
                .orElseThrow(() -> new TestScenarioNotFoundException(id));
        scenario.setName(request.name());
        scenario.setCount(request.count());
        scenario.setEntries(toEntries(request.entries()));
        return toDetail(testScenarioRepository.save(scenario));
    }

    @Transactional
    public void delete(Long id) {
        testScenarioRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public int getScenarioCount(Long scenarioId) {
        return testScenarioRepository.findById(scenarioId)
                .orElseThrow(() -> new TestScenarioNotFoundException(scenarioId))
                .getCount();
    }

    @Transactional(readOnly = true)
    public List<ScenarioMessage> buildMessagePool(Long scenarioId) {
        var scenario = testScenarioRepository.findById(scenarioId)
                .orElseThrow(() -> new TestScenarioNotFoundException(scenarioId));
        var entries = scenario.getEntries();
        if (entries == null || entries.isEmpty()) {
            return List.of();
        }

        int count = scenario.getCount();
        var pool = new ArrayList<ScenarioMessage>(count);
        int totalAllocated = 0;
        for (int i = 0; i < entries.size(); i++) {
            var entry = entries.get(i);
            var messageContent = buildMessage(entry);
            int allocated = i == entries.size() - 1
                    ? count - totalAllocated
                    : (int) Math.floor(entry.percentage() / 100.0 * count);
            for (int j = 0; j < allocated; j++) {
                pool.add(new ScenarioMessage(messageContent));
            }
            totalAllocated += allocated;
        }
        return pool;
    }

    private String buildMessage(TestScenario.ScenarioEntry entry) {
        var fields = entry.headerFields();
        if (fields == null || fields.isEmpty()) {
            return entry.content() != null ? entry.content() : "";
        }
        var header = new StringBuilder();
        for (var field : fields) {
            var val = resolveFieldValue(field);
            var padChar = field.paddingChar() != null && !field.paddingChar().isEmpty()
                    ? field.paddingChar().charAt(0) : ' ';
            if (val.length() >= field.size()) {
                header.append(val, 0, field.size());
            } else {
                header.append(val);
                header.append(String.valueOf(padChar).repeat(field.size() - val.length()));
            }
        }
        var content = entry.content() != null ? entry.content() : "";
        return header + "\n" + content;
    }

    private String resolveFieldValue(TestScenario.HeaderField field) {
        if ("UUID".equals(field.type())) {
            var prefix = field.uuidPrefix() != null ? field.uuidPrefix() : "";
            var separator = field.uuidSeparator() != null ? field.uuidSeparator() : "-";
            return prefix + separator + UUID.randomUUID();
        }
        return field.value() != null ? field.value() : "";
    }

    private TestScenarioDetail toDetail(TestScenario scenario) {
        var entryDtos = scenario.getEntries() == null ? List.<ScenarioEntryDto>of()
                : scenario.getEntries().stream()
                        .map(e -> new ScenarioEntryDto(e.testCaseId(), e.content(), e.percentage(),
                                e.headerFields() == null ? List.of()
                                        : e.headerFields().stream()
                                                .map(f -> new HeaderFieldDto(f.name(), f.size(), f.value(),
                                f.type(), f.paddingChar(), f.uuidPrefix(), f.uuidSeparator()))
                                                .toList(),
                                e.responseTemplateId()))
                        .toList();
        return new TestScenarioDetail(scenario.getId(), scenario.getName(), scenario.getCount(),
                entryDtos, scenario.getCreatedAt().toString(), scenario.getUpdatedAt().toString());
    }

    private List<TestScenario.ScenarioEntry> toEntries(List<ScenarioEntryDto> dtos) {
        if (dtos == null) {
            return new ArrayList<>();
        }
        return dtos.stream()
                .map(d -> new TestScenario.ScenarioEntry(d.testCaseId(), d.content(), d.percentage(),
                        d.headerFields() == null ? List.of()
                                : d.headerFields().stream()
                                        .map(f -> new TestScenario.HeaderField(f.name(), f.size(), f.value(),
                                f.type(), f.paddingChar(), f.uuidPrefix(), f.uuidSeparator()))
                                        .toList(),
                        d.responseTemplateId()))
                .toList();
    }

    public record TestScenarioSummary(Long id, String name, int count, String updatedAt) {
    }

    public record TestScenarioDetail(Long id, String name, int count, List<ScenarioEntryDto> entries,
                                     String createdAt, String updatedAt) {
    }

    public record HeaderFieldDto(String name, int size, String value, String type,
                                 String paddingChar, String uuidPrefix, String uuidSeparator) {
    }

    public record ScenarioEntryDto(Long testCaseId, String content, int percentage, List<HeaderFieldDto> headerFields,
                                   Long responseTemplateId) {
    }

    public record TestScenarioRequest(String name, int count, List<ScenarioEntryDto> entries) {
    }

    public record ScenarioMessage(String content) {
    }
}
