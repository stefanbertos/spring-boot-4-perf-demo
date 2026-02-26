package com.example.perftester.persistence;

import com.example.perftester.perf.ThinkTimeConfig;
import com.example.perftester.perf.ThresholdDef;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TestScenarioService {

    private static final ObjectMapper MAPPER = new ObjectMapper();

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
        applyRequest(request, scenario);
        return toDetail(testScenarioRepository.save(scenario));
    }

    @Transactional
    public TestScenarioDetail update(Long id, TestScenarioRequest request) {
        var scenario = testScenarioRepository.findById(id)
                .orElseThrow(() -> new TestScenarioNotFoundException(id));
        applyRequest(request, scenario);
        return toDetail(testScenarioRepository.save(scenario));
    }

    @Transactional
    public void delete(Long id) {
        testScenarioRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public List<TestScenarioDetail> listScheduledEnabled() {
        return testScenarioRepository.findByScheduledEnabledTrue().stream()
                .map(this::toDetail)
                .toList();
    }

    @Transactional(readOnly = true)
    public int getScenarioCount(Long scenarioId) {
        return testScenarioRepository.findById(scenarioId)
                .orElseThrow(() -> new TestScenarioNotFoundException(scenarioId))
                .getCount();
    }

    @Transactional(readOnly = true)
    public int getWarmupCount(Long scenarioId) {
        return testScenarioRepository.findById(scenarioId)
                .orElseThrow(() -> new TestScenarioNotFoundException(scenarioId))
                .getWarmupCount();
    }

    @Transactional(readOnly = true)
    public Optional<ThinkTimeConfig> getThinkTimeConfig(Long scenarioId) {
        var json = testScenarioRepository.findById(scenarioId)
                .orElseThrow(() -> new TestScenarioNotFoundException(scenarioId))
                .getThinkTimeJson();
        if (json == null || json.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(MAPPER.readValue(json, ThinkTimeConfig.class));
        } catch (JsonProcessingException e) {
            log.warn("Failed to parse think time config for scenario {}: {}", scenarioId, e.getMessage());
            return Optional.empty();
        }
    }

    @Transactional(readOnly = true)
    public List<ThresholdDef> getScenarioThresholds(Long scenarioId) {
        var json = testScenarioRepository.findById(scenarioId)
                .orElseThrow(() -> new TestScenarioNotFoundException(scenarioId))
                .getThresholdsJson();
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return MAPPER.readValue(json, new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            log.warn("Failed to parse thresholds for scenario {}: {}", scenarioId, e.getMessage());
            return List.of();
        }
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
            int allocated = i == entries.size() - 1
                    ? count - totalAllocated
                    : (int) Math.floor(entry.percentage() / 100.0 * count);
            for (int j = 0; j < allocated; j++) {
                pool.add(buildScenarioMessage(entry));
            }
            totalAllocated += allocated;
        }
        return pool;
    }

    private void applyRequest(TestScenarioRequest request, TestScenario scenario) {
        scenario.setName(request.name());
        scenario.setCount(request.count());
        scenario.setEntries(toEntries(request.entries()));
        scenario.setScheduledEnabled(request.scheduledEnabled());
        scenario.setScheduledTime(request.scheduledTime());
        scenario.setWarmupCount(request.warmupCount());
        scenario.setTestType(request.testType());
        scenario.setInfraProfileId(request.infraProfileId());
        scenario.setThinkTimeJson(serializeJson(request.thinkTime()));
        scenario.setThresholdsJson(serializeJson(request.thresholds()));
    }

    private String serializeJson(Object obj) {
        if (obj == null) {
            return null;
        }
        try {
            return MAPPER.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize object: {}", e.getMessage());
            return null;
        }
    }

    private ScenarioMessage buildScenarioMessage(TestScenario.ScenarioEntry entry) {
        var fields = entry.headerFields();
        var content = entry.content() != null ? entry.content() : "";
        if (fields == null || fields.isEmpty()) {
            return new ScenarioMessage(content, Map.of(), null);
        }
        var header = new StringBuilder();
        var jmsProperties = new LinkedHashMap<String, String>();
        String transactionId = null;
        int messageLength = content.length();
        for (var field : fields) {
            var val = resolveFieldValue(field, messageLength);
            var padChar = field.paddingChar() != null && !field.paddingChar().isEmpty()
                    ? field.paddingChar().charAt(0) : ' ';
            if (val.length() >= field.size()) {
                header.append(val, 0, field.size());
            } else {
                header.append(val);
                header.append(String.valueOf(padChar).repeat(field.size() - val.length()));
            }
            if ("TRANSACTION_ID".equals(field.type()) || field.correlationKey()) {
                jmsProperties.put(field.name(), val);
                if (transactionId == null) {
                    transactionId = val;
                }
            }
        }
        return new ScenarioMessage(header + "\n" + content, Map.copyOf(jmsProperties), transactionId);
    }

    private String resolveFieldValue(TestScenario.HeaderField field, int messageLength) {
        if ("TRANSACTION_ID".equals(field.type())) {
            return UUID.randomUUID().toString();
        }
        if ("UUID".equals(field.type())) {
            var prefix = field.uuidPrefix() != null ? field.uuidPrefix() : "";
            var separator = field.uuidSeparator() != null ? field.uuidSeparator() : "-";
            return prefix + separator + UUID.randomUUID();
        }
        if ("MESSAGE_LENGTH".equals(field.type())) {
            return String.valueOf(messageLength);
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
                                f.type(), f.paddingChar(), f.uuidPrefix(), f.uuidSeparator(),
                                f.correlationKey()))
                                                .toList()))
                        .toList();
        var thinkTime = parseThinkTime(scenario.getThinkTimeJson());
        var thresholds = parseThresholds(scenario.getThresholdsJson());
        return new TestScenarioDetail(scenario.getId(), scenario.getName(), scenario.getCount(),
                entryDtos, scenario.isScheduledEnabled(), scenario.getScheduledTime(),
                scenario.getWarmupCount(), scenario.getTestType(), scenario.getInfraProfileId(),
                thinkTime, thresholds,
                scenario.getCreatedAt().toString(), scenario.getUpdatedAt().toString());
    }

    private ThinkTimeConfig parseThinkTime(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return MAPPER.readValue(json, ThinkTimeConfig.class);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    private List<ThresholdDef> parseThresholds(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return MAPPER.readValue(json, new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            return List.of();
        }
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
                                f.type(), f.paddingChar(), f.uuidPrefix(), f.uuidSeparator(),
                                f.correlationKey()))
                                        .toList()))
                .toList();
    }

    public record TestScenarioSummary(Long id, String name, int count, String updatedAt) {
    }

    public record TestScenarioDetail(Long id, String name, int count, List<ScenarioEntryDto> entries,
                                     boolean scheduledEnabled, String scheduledTime,
                                     int warmupCount, String testType, Long infraProfileId,
                                     ThinkTimeConfig thinkTime, List<ThresholdDef> thresholds,
                                     String createdAt, String updatedAt) {
    }

    public record HeaderFieldDto(String name, int size, String value, String type,
                                 String paddingChar, String uuidPrefix, String uuidSeparator,
                                 boolean correlationKey) {
    }

    public record ScenarioEntryDto(Long testCaseId, String content, int percentage,
                                   List<HeaderFieldDto> headerFields) {
    }

    public record TestScenarioRequest(String name, int count, List<ScenarioEntryDto> entries,
                                      boolean scheduledEnabled, String scheduledTime,
                                      int warmupCount, String testType, Long infraProfileId,
                                      ThinkTimeConfig thinkTime, List<ThresholdDef> thresholds) {
    }

    public record ScenarioMessage(String content, Map<String, String> jmsProperties, String transactionId) {
    }
}
