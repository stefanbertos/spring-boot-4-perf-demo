package com.example.apigateway.componenttest.steps;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.web.reactive.server.WebTestClient;

public class GatewayRoutingSteps {

    @LocalServerPort
    private int port;

    @Autowired
    private WireMockServer wireMockServer;

    private WebTestClient webTestClient;
    private WebTestClient.ResponseSpec responseSpec;

    @Before
    public void setup() {
        webTestClient = WebTestClient.bindToServer()
                .baseUrl("http://localhost:" + port)
                .build();
        wireMockServer.resetAll();
    }

    @Given("the backend {string} is stubbed to return {int}")
    public void theBackendIsStubbedToReturn(String backend, int statusCode) {
        wireMockServer.stubFor(WireMock.any(WireMock.anyUrl())
                .willReturn(WireMock.aResponse()
                        .withStatus(statusCode)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"status\":\"OK\"}")));
    }

    @When("a GET request is sent to the gateway at {string}")
    public void aGetRequestIsSentToTheGateway(String path) {
        responseSpec = webTestClient.get()
                .uri(path)
                .exchange();
    }

    @Then("the response status is {int}")
    public void theResponseStatusIs(int expectedStatus) {
        responseSpec.expectStatus().isEqualTo(expectedStatus);
    }
}
