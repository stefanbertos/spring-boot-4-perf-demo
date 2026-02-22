package com.example.perftester.persistence;

import com.example.perftester.admin.IbmMqAdminService;
import com.example.perftester.admin.KafkaAdminService;
import com.example.perftester.admin.LoggingAdminService;
import com.example.perftester.kubernetes.KubernetesService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.logging.LogLevel;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class InfraProfileService {

    private final InfraProfileRepository repository;
    private final LoggingAdminService loggingAdminService;
    private final KafkaAdminService kafkaAdminService;
    private final KubernetesService kubernetesService;
    private final IbmMqAdminService ibmMqAdminService;

    @Transactional(readOnly = true)
    public List<InfraProfileSummary> listAll() {
        return repository.findAllByOrderByNameAsc().stream()
                .map(p -> new InfraProfileSummary(p.getId(), p.getName(), p.getUpdatedAt().toString()))
                .toList();
    }

    @Transactional(readOnly = true)
    public InfraProfileDetail getById(Long id) {
        return toDetail(findOrThrow(id));
    }

    @Transactional
    public InfraProfileDetail create(InfraProfileRequest request) {
        var profile = new InfraProfile();
        applyRequest(profile, request);
        return toDetail(repository.save(profile));
    }

    @Transactional
    public InfraProfileDetail update(Long id, InfraProfileRequest request) {
        var profile = findOrThrow(id);
        if (repository.existsByNameAndIdNot(request.name(), id)) {
            throw new IllegalArgumentException("A profile named '" + request.name() + "' already exists");
        }
        applyRequest(profile, request);
        return toDetail(repository.save(profile));
    }

    @Transactional
    public void delete(Long id) {
        var profile = findOrThrow(id);
        repository.delete(profile);
    }

    public ApplyResult applyProfile(Long id) {
        var profile = findOrThrow(id);
        var applied = new ArrayList<String>();
        var errors = new ArrayList<String>();
        applyLogLevels(profile.getLogLevels(), applied, errors);
        applyKafkaTopics(profile.getKafkaTopics(), applied, errors);
        applyKubernetesReplicas(profile.getKubernetesReplicas(), applied, errors);
        applyIbmMqQueues(profile.getIbmMqQueues(), applied, errors);
        log.info("Applied infra profile '{}': {} applied, {} errors", profile.getName(), applied.size(), errors.size());
        return new ApplyResult(applied, errors);
    }

    private void applyLogLevels(Map<String, String> logLevels, List<String> applied, List<String> errors) {
        for (var entry : logLevels.entrySet()) {
            try {
                var level = LogLevel.valueOf(entry.getValue().toUpperCase());
                loggingAdminService.setLogLevel(entry.getKey(), level);
                applied.add("log:" + entry.getKey() + "=" + entry.getValue());
            } catch (Exception e) {
                errors.add("log:" + entry.getKey() + ": " + e.getMessage());
            }
        }
    }

    private void applyKafkaTopics(Map<String, Integer> kafkaTopics, List<String> applied, List<String> errors) {
        for (var entry : kafkaTopics.entrySet()) {
            try {
                kafkaAdminService.resizeTopic(entry.getKey(), entry.getValue());
                applied.add("kafka:" + entry.getKey() + "=" + entry.getValue());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                errors.add("kafka:" + entry.getKey() + ": interrupted");
            } catch (Exception e) {
                errors.add("kafka:" + entry.getKey() + ": " + e.getMessage());
            }
        }
    }

    private void applyKubernetesReplicas(Map<String, Integer> replicas, List<String> applied, List<String> errors) {
        for (var entry : replicas.entrySet()) {
            try {
                kubernetesService.scaleDeployment(entry.getKey(), entry.getValue());
                applied.add("k8s:" + entry.getKey() + "=" + entry.getValue());
            } catch (Exception e) {
                errors.add("k8s:" + entry.getKey() + ": " + e.getMessage());
            }
        }
    }

    private void applyIbmMqQueues(Map<String, Integer> queues, List<String> applied, List<String> errors) {
        for (var entry : queues.entrySet()) {
            try {
                ibmMqAdminService.changeQueueMaxDepth(entry.getKey(), entry.getValue());
                applied.add("mq:" + entry.getKey() + "=" + entry.getValue());
            } catch (Exception e) {
                errors.add("mq:" + entry.getKey() + ": " + e.getMessage());
            }
        }
    }

    private InfraProfile findOrThrow(Long id) {
        return repository.findById(id).orElseThrow(() -> new InfraProfileNotFoundException(id));
    }

    private void applyRequest(InfraProfile profile, InfraProfileRequest request) {
        profile.setName(request.name());
        profile.setLogLevels(new HashMap<>(request.logLevels()));
        profile.setKafkaTopics(new HashMap<>(request.kafkaTopics()));
        profile.setKubernetesReplicas(new HashMap<>(request.kubernetesReplicas()));
        profile.setIbmMqQueues(new HashMap<>(request.ibmMqQueues()));
    }

    private InfraProfileDetail toDetail(InfraProfile p) {
        return new InfraProfileDetail(
                p.getId(), p.getName(),
                p.getLogLevels(), p.getKafkaTopics(), p.getKubernetesReplicas(), p.getIbmMqQueues(),
                p.getCreatedAt().toString(), p.getUpdatedAt().toString());
    }

    public record InfraProfileSummary(long id, String name, String updatedAt) {
    }

    public record InfraProfileDetail(
            long id, String name,
            Map<String, String> logLevels,
            Map<String, Integer> kafkaTopics,
            Map<String, Integer> kubernetesReplicas,
            Map<String, Integer> ibmMqQueues,
            String createdAt, String updatedAt) {
    }

    public record InfraProfileRequest(
            String name,
            Map<String, String> logLevels,
            Map<String, Integer> kafkaTopics,
            Map<String, Integer> kubernetesReplicas,
            Map<String, Integer> ibmMqQueues) {
    }

    public record ApplyResult(List<String> applied, List<String> errors) {
    }
}
