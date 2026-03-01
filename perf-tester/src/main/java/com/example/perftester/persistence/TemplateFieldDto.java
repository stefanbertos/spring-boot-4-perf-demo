package com.example.perftester.persistence;

public record TemplateFieldDto(String name, int size, String value, String type,
                               String paddingChar, String uuidPrefix, String uuidSeparator,
                               boolean correlationKey) {
}
