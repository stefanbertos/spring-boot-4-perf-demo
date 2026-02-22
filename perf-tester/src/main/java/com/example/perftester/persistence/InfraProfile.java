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
import java.util.HashMap;
import java.util.Map;

@Entity
@Table(name = "infra_profile")
@Getter
@Setter
public class InfraProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "infra_profile_seq")
    @SequenceGenerator(name = "infra_profile_seq", sequenceName = "infra_profile_seq", allocationSize = 1)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(name = "log_levels", columnDefinition = "text")
    @Convert(converter = StringMapConverter.class)
    private Map<String, String> logLevels = new HashMap<>();

    @Column(name = "kafka_topics", columnDefinition = "text")
    @Convert(converter = IntegerMapConverter.class)
    private Map<String, Integer> kafkaTopics = new HashMap<>();

    @Column(name = "kubernetes_replicas", columnDefinition = "text")
    @Convert(converter = IntegerMapConverter.class)
    private Map<String, Integer> kubernetesReplicas = new HashMap<>();

    @Column(name = "ibm_mq_queues", columnDefinition = "text")
    @Convert(converter = IntegerMapConverter.class)
    private Map<String, Integer> ibmMqQueues = new HashMap<>();

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

    @Converter
    public static class StringMapConverter implements AttributeConverter<Map<String, String>, String> {

        private static final ObjectMapper MAPPER = new ObjectMapper();
        private static final TypeReference<Map<String, String>> TYPE = new TypeReference<>() {};

        @Override
        public String convertToDatabaseColumn(Map<String, String> attribute) {
            try {
                return attribute == null ? "{}" : MAPPER.writeValueAsString(attribute);
            } catch (JsonProcessingException e) {
                return "{}";
            }
        }

        @Override
        public Map<String, String> convertToEntityAttribute(String dbData) {
            try {
                return dbData == null || dbData.isEmpty() ? new HashMap<>() : MAPPER.readValue(dbData, TYPE);
            } catch (JsonProcessingException e) {
                return new HashMap<>();
            }
        }
    }

    @Converter
    public static class IntegerMapConverter implements AttributeConverter<Map<String, Integer>, String> {

        private static final ObjectMapper MAPPER = new ObjectMapper();
        private static final TypeReference<Map<String, Integer>> TYPE = new TypeReference<>() {};

        @Override
        public String convertToDatabaseColumn(Map<String, Integer> attribute) {
            try {
                return attribute == null ? "{}" : MAPPER.writeValueAsString(attribute);
            } catch (JsonProcessingException e) {
                return "{}";
            }
        }

        @Override
        public Map<String, Integer> convertToEntityAttribute(String dbData) {
            try {
                return dbData == null || dbData.isEmpty() ? new HashMap<>() : MAPPER.readValue(dbData, TYPE);
            } catch (JsonProcessingException e) {
                return new HashMap<>();
            }
        }
    }
}
