package com.example.perftester.persistence;

public class TestCaseNameConflictException extends RuntimeException {

    public TestCaseNameConflictException(String name) {
        super("Test case with name '" + name + "' already exists");
    }
}
