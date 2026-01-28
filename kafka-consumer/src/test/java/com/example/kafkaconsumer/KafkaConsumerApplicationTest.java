package com.example.kafkaconsumer;

import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mockStatic;

class KafkaConsumerApplicationTest {

    @Test
    void applicationClassShouldBeInstantiable() {
        KafkaConsumerApplication app = new KafkaConsumerApplication();
        assertNotNull(app);
    }

    @Test
    void mainMethodShouldCallSpringApplicationRun() {
        try (var mockedSpringApp = mockStatic(SpringApplication.class)) {
            mockedSpringApp.when(() -> SpringApplication.run(KafkaConsumerApplication.class, new String[]{}))
                    .thenReturn(null);

            KafkaConsumerApplication.main(new String[]{});

            mockedSpringApp.verify(() -> SpringApplication.run(KafkaConsumerApplication.class, new String[]{}));
        }
    }
}
