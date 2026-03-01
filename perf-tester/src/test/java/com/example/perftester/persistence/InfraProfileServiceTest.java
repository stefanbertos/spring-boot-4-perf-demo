package com.example.perftester.persistence;

import com.example.perftester.admin.IbmMqAdminService;
import com.example.perftester.admin.KafkaAdminService;
import com.example.perftester.admin.LoggingAdminService;
import com.example.perftester.kubernetes.KubernetesService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.logging.LogLevel;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InfraProfileServiceTest {

    @Mock
    private InfraProfileRepository repository;

    @Mock
    private LoggingAdminService loggingAdminService;

    @Mock
    private KafkaAdminService kafkaAdminService;

    @Mock
    private KubernetesService kubernetesService;

    @Mock
    private IbmMqAdminService ibmMqAdminService;

    @InjectMocks
    private InfraProfileService infraProfileService;

    private InfraProfile profileWithId(Long id) {
        var profile = new InfraProfile();
        profile.setId(id);
        profile.setName("profile-" + id);
        profile.setCreatedAt(Instant.now());
        profile.setUpdatedAt(Instant.now());
        return profile;
    }

    private InfraProfileRequest emptyRequest(String name) {
        return new InfraProfileRequest(name, Map.of(), Map.of(), Map.of(), Map.of());
    }

    @Test
    void listAllShouldReturnSummaries() {
        when(repository.findAllByOrderByNameAsc()).thenReturn(List.of(profileWithId(1L)));

        var result = infraProfileService.listAll();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo(1L);
        assertThat(result.get(0).name()).isEqualTo("profile-1");
    }

    @Test
    void getByIdShouldReturnDetail() {
        when(repository.findById(1L)).thenReturn(Optional.of(profileWithId(1L)));

        var result = infraProfileService.getById(1L);

        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.name()).isEqualTo("profile-1");
    }

    @Test
    void getByIdShouldThrowWhenNotFound() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> infraProfileService.getById(99L))
                .isInstanceOf(InfraProfileNotFoundException.class);
    }

    @Test
    void createShouldSaveAndReturnDetail() {
        var request = emptyRequest("new-profile");
        var saved = profileWithId(2L);
        saved.setName("new-profile");
        when(repository.save(any())).thenReturn(saved);

        var result = infraProfileService.create(request);

        verify(repository).save(any(InfraProfile.class));
        assertThat(result.name()).isEqualTo("new-profile");
    }

    @Test
    void updateShouldModifyAndReturnDetail() {
        var profile = profileWithId(1L);
        var request = emptyRequest("updated");
        var saved = profileWithId(1L);
        saved.setName("updated");
        when(repository.findById(1L)).thenReturn(Optional.of(profile));
        when(repository.existsByNameAndIdNot("updated", 1L)).thenReturn(false);
        when(repository.save(profile)).thenReturn(saved);

        var result = infraProfileService.update(1L, request);

        assertThat(result.name()).isEqualTo("updated");
    }

    @Test
    void updateShouldThrowWhenNameConflicts() {
        when(repository.findById(1L)).thenReturn(Optional.of(profileWithId(1L)));
        when(repository.existsByNameAndIdNot("taken", 1L)).thenReturn(true);

        assertThatThrownBy(() -> infraProfileService.update(1L, emptyRequest("taken")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("taken");
    }

    @Test
    void updateShouldThrowWhenNotFound() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> infraProfileService.update(99L, emptyRequest("name")))
                .isInstanceOf(InfraProfileNotFoundException.class);
    }

    @Test
    void deleteShouldCallDelete() {
        var profile = profileWithId(1L);
        when(repository.findById(1L)).thenReturn(Optional.of(profile));

        infraProfileService.delete(1L);

        verify(repository).delete(profile);
    }

    @Test
    void deleteShouldThrowWhenNotFound() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> infraProfileService.delete(99L))
                .isInstanceOf(InfraProfileNotFoundException.class);
    }

    @Test
    void applyProfileShouldApplyLogLevels() {
        var profile = profileWithId(1L);
        profile.setLogLevels(Map.of("com.example", "DEBUG"));
        when(repository.findById(1L)).thenReturn(Optional.of(profile));

        var result = infraProfileService.applyProfile(1L);

        verify(loggingAdminService).setLogLevel("com.example", LogLevel.DEBUG);
        assertThat(result.applied()).containsExactly("log:com.example=DEBUG");
        assertThat(result.errors()).isEmpty();
    }

    @Test
    void applyProfileShouldRecordErrorWhenLogLevelInvalid() {
        var profile = profileWithId(1L);
        profile.setLogLevels(Map.of("com.example", "INVALID_LEVEL"));
        when(repository.findById(1L)).thenReturn(Optional.of(profile));

        var result = infraProfileService.applyProfile(1L);

        assertThat(result.applied()).isEmpty();
        assertThat(result.errors()).hasSize(1);
        assertThat(result.errors().get(0)).startsWith("log:com.example:");
    }

    @Test
    void applyProfileShouldRecordErrorWhenKafkaFails()
            throws InterruptedException, ExecutionException, TimeoutException {
        var profile = profileWithId(1L);
        profile.setKafkaTopics(Map.of("mq-requests", 3));
        when(repository.findById(1L)).thenReturn(Optional.of(profile));
        doThrow(new ExecutionException("Kafka error", new RuntimeException()))
                .when(kafkaAdminService).resizeTopic("mq-requests", 3);

        var result = infraProfileService.applyProfile(1L);

        assertThat(result.applied()).isEmpty();
        assertThat(result.errors()).hasSize(1);
        assertThat(result.errors().get(0)).startsWith("kafka:mq-requests:");
    }

    @Test
    void applyProfileShouldApplyAllCategories()
            throws InterruptedException, ExecutionException, TimeoutException {
        var profile = profileWithId(1L);
        profile.setLogLevels(Map.of("com.example", "INFO"));
        profile.setKafkaTopics(Map.of("mq-requests", 3));
        profile.setKubernetesReplicas(Map.of("kafka-consumer", 2));
        profile.setIbmMqQueues(Map.of("DEV.QUEUE.1", 50000));
        when(repository.findById(1L)).thenReturn(Optional.of(profile));

        var result = infraProfileService.applyProfile(1L);

        assertThat(result.applied()).hasSize(4);
        assertThat(result.errors()).isEmpty();
    }
}
