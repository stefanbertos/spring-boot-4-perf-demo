package com.example.perftester;

import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mockStatic;

class PerfTesterApplicationTest {

    @Test
    void applicationClassShouldBeInstantiable() {
        PerfTesterApplication app = new PerfTesterApplication();
        assertNotNull(app);
    }

    @Test
    void mainMethodShouldCallSpringApplicationRun() {
        try (var mockedSpringApp = mockStatic(SpringApplication.class)) {
            mockedSpringApp.when(() -> SpringApplication.run(PerfTesterApplication.class, new String[]{}))
                    .thenReturn(null);

            PerfTesterApplication.main(new String[]{});

            mockedSpringApp.verify(() -> SpringApplication.run(PerfTesterApplication.class, new String[]{}));
        }
    }
}
