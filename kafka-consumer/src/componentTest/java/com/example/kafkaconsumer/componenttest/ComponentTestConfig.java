package com.example.kafkaconsumer.componenttest;

import com.example.kafkaconsumer.componenttest.containers.KafkaContainerConfig;
import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@CucumberContextConfiguration
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("componenttest")
@Import(KafkaContainerConfig.class)
public class ComponentTestConfig {
}
