package com.example.ibmmqconsumer.componenttest;

import com.example.ibmmqconsumer.componenttest.containers.IbmMqContainerConfig;
import com.example.ibmmqconsumer.componenttest.containers.KafkaContainerConfig;
import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@CucumberContextConfiguration
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("componenttest")
@Import({KafkaContainerConfig.class, IbmMqContainerConfig.class})
public class ComponentTestConfig {
}
