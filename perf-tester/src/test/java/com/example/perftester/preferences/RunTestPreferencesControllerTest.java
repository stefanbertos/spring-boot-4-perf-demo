package com.example.perftester.preferences;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RunTestPreferencesControllerTest {

    @Mock
    private RunTestPreferencesService service;

    @InjectMocks
    private RunTestPreferencesController controller;

    @Test
    void getShouldDelegateToService() {
        var response = new RunTestPreferencesResponse(true, false, true, false, true, false);
        when(service.get()).thenReturn(response);

        var result = controller.get();

        assertThat(result).isEqualTo(response);
    }

    @Test
    void updateShouldDelegateToService() {
        var request = new RunTestPreferencesRequest(true, true, false, true, false, true);
        var response = new RunTestPreferencesResponse(true, true, false, true, false, true);
        when(service.update(request)).thenReturn(response);

        var result = controller.update(request);

        assertThat(result).isEqualTo(response);
        verify(service).update(request);
    }
}

