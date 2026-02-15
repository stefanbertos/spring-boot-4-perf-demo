package com.example.perftester.rest;

import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleIllegalArgumentReturnsBadRequest() {
        var result = handler.handleIllegalArgument(new IllegalArgumentException("bad input"));

        assertThat(result.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(result.getDetail()).isEqualTo("bad input");
        assertThat(result.getTitle()).isEqualTo("Invalid Request");
    }

    @Test
    void handleConstraintViolationReturnsBadRequest() {
        var result = handler.handleConstraintViolation(new ConstraintViolationException("validation failed", Set.of()));

        assertThat(result.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(result.getTitle()).isEqualTo("Validation Failed");
    }

    @Test
    void handleGenericExceptionReturnsInternalServerError() {
        var result = handler.handleGenericException(new RuntimeException("something broke"));

        assertThat(result.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
        assertThat(result.getDetail()).isEqualTo("something broke");
        assertThat(result.getTitle()).isEqualTo("Internal Server Error");
    }
}
