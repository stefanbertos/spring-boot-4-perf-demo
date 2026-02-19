package com.example.perftester.persistence;

public class TestCaseNotFoundException extends RuntimeException {

    public TestCaseNotFoundException(long id) {
        super("Test case not found with id: " + id);
    }
}
