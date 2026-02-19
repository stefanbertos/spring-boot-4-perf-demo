package com.example.perftester.persistence;

public class TestRunNotFoundException extends RuntimeException {

    public TestRunNotFoundException(long id) {
        super("Test run not found with id: " + id);
    }
}
