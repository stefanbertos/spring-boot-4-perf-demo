package com.example.perftester.perf;

import com.example.perftester.persistence.ResponseTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
public record MessageExpectation(
        String testCaseName,
        @Nullable List<ResponseTemplate.ResponseField> responseFields,
        Map<String, String> correlationKeyValues) {

    public boolean hasValidation() {
        return responseFields != null && !responseFields.isEmpty();
    }

    public List<String> validate(@Nullable String responseBody) {
        if (responseBody == null) {
            return List.of("Response body is null");
        }
        var failures = new ArrayList<String>();
        var offset = 0;
        for (var field : responseFields) {
            if (offset >= responseBody.length()) {
                failures.add("Field '" + field.name() + "': response too short at offset " + offset);
                break;
            }
            var actual = responseBody.substring(offset, Math.min(offset + field.size(), responseBody.length()));
            switch (field.type()) {
                case "IGNORE" -> { /* skip */ }
                case "STATIC" -> checkStatic(field, actual, failures);
                case "REGEX" -> checkRegex(field, actual, failures);
                case "ECHO" -> checkEcho(field, actual, failures);
                default -> log.warn("Unknown response field type '{}' for field '{}'", field.type(), field.name());
            }
            offset += field.size();
        }
        return failures;
    }

    private void checkStatic(ResponseTemplate.ResponseField field, String actual, List<String> failures) {
        var expected = padOrTruncate(field.value(), field.size(), field.paddingChar());
        if (!actual.equals(expected)) {
            failures.add("Field '" + field.name() + "': expected '" + expected + "' got '" + actual + "'");
        }
    }

    private void checkRegex(ResponseTemplate.ResponseField field, String actual, List<String> failures) {
        var pattern = field.value() != null ? field.value() : "";
        if (!actual.strip().matches(pattern)) {
            failures.add("Field '" + field.name() + "': '" + actual.strip() + "' does not match '" + pattern + "'");
        }
    }

    private void checkEcho(ResponseTemplate.ResponseField field, String actual, List<String> failures) {
        var keyName = field.value();
        var echoValue = keyName != null ? correlationKeyValues.get(keyName) : null;
        if (echoValue == null) {
            failures.add("Field '" + field.name() + "': no correlation key '" + keyName + "'");
            return;
        }
        var expected = padOrTruncate(echoValue, field.size(), field.paddingChar());
        if (!actual.equals(expected)) {
            failures.add("Field '" + field.name() + "': expected echo '" + expected + "' got '" + actual + "'");
        }
    }

    private String padOrTruncate(String value, int size, String paddingChar) {
        var effective = value != null ? value : "";
        var padChar = paddingChar != null && !paddingChar.isEmpty() ? paddingChar : " ";
        if (effective.length() >= size) {
            return effective.substring(0, size);
        }
        return effective + padChar.repeat(size - effective.length());
    }
}
