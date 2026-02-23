package com.example.perftester.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Converter;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "header_template")
@Getter
@Setter
public class HeaderTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "header_template_seq")
    @SequenceGenerator(name = "header_template_seq", sequenceName = "header_template_seq", allocationSize = 1)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(name = "fields", columnDefinition = "text")
    @Convert(converter = FieldListConverter.class)
    private List<TemplateField> fields = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        var now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public record TemplateField(String name, int size, String value, String type,
                                String paddingChar, String uuidPrefix, String uuidSeparator,
                                boolean correlationKey) {
    }

    @Converter
    public static class FieldListConverter implements AttributeConverter<List<TemplateField>, String> {

        private static final ObjectMapper MAPPER = new ObjectMapper();
        private static final TypeReference<List<TemplateField>> TYPE = new TypeReference<>() {};

        @Override
        public String convertToDatabaseColumn(List<TemplateField> attribute) {
            try {
                return attribute == null ? "[]" : MAPPER.writeValueAsString(attribute);
            } catch (JsonProcessingException e) {
                return "[]";
            }
        }

        @Override
        public List<TemplateField> convertToEntityAttribute(String dbData) {
            try {
                return dbData == null || dbData.isEmpty() ? new ArrayList<>() : MAPPER.readValue(dbData, TYPE);
            } catch (JsonProcessingException e) {
                return new ArrayList<>();
            }
        }
    }
}
