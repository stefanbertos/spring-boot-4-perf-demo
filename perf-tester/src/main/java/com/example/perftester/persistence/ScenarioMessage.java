package com.example.perftester.persistence;

import org.springframework.lang.Nullable;

import java.util.List;
import java.util.Map;

public record ScenarioMessage(
        String content,
        Map<String, String> jmsProperties,
        String transactionId,
        String testCaseName,
        @Nullable List<ResponseTemplate.ResponseField> responseFields) {
}
