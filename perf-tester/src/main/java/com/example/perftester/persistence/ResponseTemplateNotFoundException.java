package com.example.perftester.persistence;

public class ResponseTemplateNotFoundException extends RuntimeException {

    public ResponseTemplateNotFoundException(long id) {
        super("Response template not found with id: " + id);
    }
}
