package com.example.perftester.preferences;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RunTestPreferencesServiceTest {

    @Mock
    private RunTestPreferencesRepository repository;

    @InjectMocks
    private RunTestPreferencesService service;

    private RunTestPreferences entity(boolean grafana, boolean prometheus, boolean kubernetes,
                                      boolean logs, boolean database, boolean debug) {
        var p = new RunTestPreferences();
        p.setId(1L);
        p.setExportGrafana(grafana);
        p.setExportPrometheus(prometheus);
        p.setExportKubernetes(kubernetes);
        p.setExportLogs(logs);
        p.setExportDatabase(database);
        p.setDebug(debug);
        return p;
    }

    @Test
    void getShouldReturnStoredPreferences() {
        when(repository.findById(1L)).thenReturn(Optional.of(entity(true, false, true, false, true, false)));

        var result = service.get();

        assertThat(result.exportGrafana()).isTrue();
        assertThat(result.exportPrometheus()).isFalse();
        assertThat(result.exportKubernetes()).isTrue();
        assertThat(result.exportLogs()).isFalse();
        assertThat(result.exportDatabase()).isTrue();
        assertThat(result.debug()).isFalse();
    }

    @Test
    void getShouldReturnDefaultsWhenNotFound() {
        when(repository.findById(1L)).thenReturn(Optional.empty());

        var result = service.get();

        assertThat(result.exportGrafana()).isFalse();
        assertThat(result.exportPrometheus()).isFalse();
        assertThat(result.exportKubernetes()).isFalse();
        assertThat(result.exportLogs()).isFalse();
        assertThat(result.exportDatabase()).isFalse();
        assertThat(result.debug()).isFalse();
    }

    @Test
    void updateShouldSaveAndReturnUpdatedPreferences() {
        var existing = entity(false, false, false, false, false, false);
        when(repository.findById(1L)).thenReturn(Optional.of(existing));
        when(repository.save(existing)).thenReturn(existing);

        var request = new RunTestPreferencesRequest(true, true, false, true, false, true);
        var result = service.update(request);

        assertThat(result.exportGrafana()).isTrue();
        assertThat(result.exportPrometheus()).isTrue();
        assertThat(result.exportKubernetes()).isFalse();
        assertThat(result.exportLogs()).isTrue();
        assertThat(result.exportDatabase()).isFalse();
        assertThat(result.debug()).isTrue();
    }

    @Test
    void updateShouldCreateRowWhenNotFound() {
        when(repository.findById(1L)).thenReturn(Optional.empty());
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var request = new RunTestPreferencesRequest(false, false, false, false, true, false);
        var result = service.update(request);

        assertThat(result.exportDatabase()).isTrue();
    }
}

