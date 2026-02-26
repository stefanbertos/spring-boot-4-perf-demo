package com.example.perftester.preferences;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RunTestPreferencesService {

    private static final long PREFERENCES_ID = 1L;

    private final RunTestPreferencesRepository repository;

    @Transactional(readOnly = true)
    public RunTestPreferencesResponse get() {
        return repository.findById(PREFERENCES_ID)
                .map(this::toResponse)
                .orElseGet(() -> new RunTestPreferencesResponse(false, false, false, false, false, false));
    }

    @Transactional
    public RunTestPreferencesResponse update(RunTestPreferencesRequest request) {
        var prefs = repository.findById(PREFERENCES_ID).orElseGet(() -> {
            var p = new RunTestPreferences();
            p.setId(PREFERENCES_ID);
            return p;
        });
        prefs.setExportGrafana(request.exportGrafana());
        prefs.setExportPrometheus(request.exportPrometheus());
        prefs.setExportKubernetes(request.exportKubernetes());
        prefs.setExportLogs(request.exportLogs());
        prefs.setExportDatabase(request.exportDatabase());
        prefs.setDebug(request.debug());
        return toResponse(repository.save(prefs));
    }

    private RunTestPreferencesResponse toResponse(RunTestPreferences prefs) {
        return new RunTestPreferencesResponse(
                prefs.isExportGrafana(),
                prefs.isExportPrometheus(),
                prefs.isExportKubernetes(),
                prefs.isExportLogs(),
                prefs.isExportDatabase(),
                prefs.isDebug());
    }
}
