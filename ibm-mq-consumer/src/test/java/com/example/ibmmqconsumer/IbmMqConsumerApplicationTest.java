package com.example.ibmmqconsumer;

import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mockStatic;

class IbmMqConsumerApplicationTest {

    @Test
    void applicationClassShouldBeInstantiable() {
        IbmMqConsumerApplication app = new IbmMqConsumerApplication();
        assertNotNull(app);
    }

    @Test
    void mainMethodShouldCallSpringApplicationRun() {
        try (var mockedSpringApp = mockStatic(SpringApplication.class)) {
            mockedSpringApp.when(() -> SpringApplication.run(IbmMqConsumerApplication.class, new String[]{}))
                    .thenReturn(null);

            IbmMqConsumerApplication.main(new String[]{});

            mockedSpringApp.verify(() -> SpringApplication.run(IbmMqConsumerApplication.class, new String[]{}));
        }
    }
}
