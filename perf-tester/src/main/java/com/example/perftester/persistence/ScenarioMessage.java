package com.example.perftester.persistence;

import java.util.Map;

public record ScenarioMessage(String content, Map<String, String> jmsProperties, String transactionId) {
}
