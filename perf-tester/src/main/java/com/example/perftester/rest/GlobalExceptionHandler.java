package com.example.perftester.rest;

import com.example.perftester.persistence.HeaderTemplateNotFoundException;
import com.example.perftester.persistence.InfraProfileNotFoundException;
import com.example.perftester.persistence.ResponseTemplateNotFoundException;
import com.example.perftester.persistence.TestCaseNameConflictException;
import com.example.perftester.persistence.TestCaseNotFoundException;
import com.example.perftester.persistence.TestRunNotFoundException;
import com.example.perftester.persistence.TestScenarioNotFoundException;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(HeaderTemplateNotFoundException.class)
    public ProblemDetail handleHeaderTemplateNotFound(HeaderTemplateNotFoundException ex) {
        var problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setTitle("Header Template Not Found");
        return problem;
    }

    @ExceptionHandler(InfraProfileNotFoundException.class)
    public ProblemDetail handleInfraProfileNotFound(InfraProfileNotFoundException ex) {
        var problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setTitle("Infra Profile Not Found");
        return problem;
    }

    @ExceptionHandler(TestScenarioNotFoundException.class)
    public ProblemDetail handleTestScenarioNotFound(TestScenarioNotFoundException ex) {
        var problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setTitle("Test Scenario Not Found");
        return problem;
    }

    @ExceptionHandler(TestCaseNotFoundException.class)
    public ProblemDetail handleTestCaseNotFound(TestCaseNotFoundException ex) {
        var problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setTitle("Test Case Not Found");
        return problem;
    }

    @ExceptionHandler(ResponseTemplateNotFoundException.class)
    public ProblemDetail handleResponseTemplateNotFound(ResponseTemplateNotFoundException ex) {
        var problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setTitle("Response Template Not Found");
        return problem;
    }

    @ExceptionHandler(TestRunNotFoundException.class)
    public ProblemDetail handleTestRunNotFound(TestRunNotFoundException ex) {
        var problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setTitle("Test Run Not Found");
        return problem;
    }

    @ExceptionHandler(TestCaseNameConflictException.class)
    public ProblemDetail handleTestCaseNameConflict(TestCaseNameConflictException ex) {
        var problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setTitle("Test Case Name Conflict");
        return problem;
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleIllegalArgument(IllegalArgumentException ex) {
        var problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setTitle("Invalid Request");
        return problem;
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ProblemDetail handleConstraintViolation(ConstraintViolationException ex) {
        var problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setTitle("Validation Failed");
        return problem;
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ProblemDetail handleNoResourceFound(NoResourceFoundException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGenericException(Exception ex) {
        log.error("Unexpected error: {}", ex.getMessage(), ex);
        var problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage());
        problem.setTitle("Internal Server Error");
        return problem;
    }
}
