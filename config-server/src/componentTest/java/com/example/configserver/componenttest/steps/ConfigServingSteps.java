package com.example.configserver.componenttest.steps;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class ConfigServingSteps {

    @LocalServerPort
    private int port;

    private Map<String, Object> responseBody;

    @Given("the config-server is running with native profile")
    public void theConfigServerIsRunning() {
        // Spring Boot test context is already started
    }

    @When("configuration for application {string} is requested")
    public void configurationForApplicationIsRequested(String appName) {
        var restClient = RestClient.create("http://localhost:" + port);
        responseBody = restClient.get()
                .uri("/{application}/default", appName)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
    }

    @SuppressWarnings("unchecked")
    @Then("the response contains property {string} with value {string}")
    public void theResponseContainsProperty(String propertyPath, String expectedValue) {
        assertThat(responseBody).isNotNull();

        var propertySources = (List<Map<String, Object>>) responseBody.get("propertySources");
        assertThat(propertySources).isNotEmpty();

        var found = propertySources.stream()
                .map(ps -> (Map<String, Object>) ps.get("source"))
                .anyMatch(source -> expectedValue.equals(String.valueOf(source.get(propertyPath))));

        assertThat(found)
                .as("Expected property '%s' with value '%s' in config response", propertyPath, expectedValue)
                .isTrue();
    }
}
