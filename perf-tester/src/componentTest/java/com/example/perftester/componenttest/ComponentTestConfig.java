package com.example.perftester.componenttest;

import com.example.perftester.componenttest.containers.IbmMqContainerConfig;
import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@CucumberContextConfiguration
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("componenttest")
@Import(IbmMqContainerConfig.class)
public class ComponentTestConfig {
}
