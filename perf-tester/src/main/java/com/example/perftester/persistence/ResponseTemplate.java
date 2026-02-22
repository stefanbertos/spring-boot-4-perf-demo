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
@Table(name = "response_template")
@Getter
@Setter
public class ResponseTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "response_template_seq")
    @SequenceGenerator(name = "response_template_seq", sequenceName = "response_template_seq", allocationSize = 1)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(name = "fields", columnDefinition = "text")
    @Convert(converter = FieldListConverter.class)
    private List<ResponseField> fields = new ArrayList<>();

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

    public record ResponseField(String name, int size, String value, String type, String paddingChar) {
    }

    @Converter
    public static class FieldListConverter implements AttributeConverter<List<ResponseField>, String> {

        private static final ObjectMapper MAPPER = new ObjectMapper();
        private static final TypeReference<List<ResponseField>> TYPE = new TypeReference<>() {};

        @Override
        public String convertToDatabaseColumn(List<ResponseField> attribute) {
            try {
                return attribute == null ? "[]" : MAPPER.writeValueAsString(attribute);
            } catch (JsonProcessingException e) {
                return "[]";
            }
        }

        @Override
        public List<ResponseField> convertToEntityAttribute(String dbData) {
            try {
                return dbData == null || dbData.isEmpty() ? new ArrayList<>() : MAPPER.readValue(dbData, TYPE);
            } catch (JsonProcessingException e) {
                return new ArrayList<>();
            }
        }
    }
}
