package com.example.perftester.persistence;

public class HeaderTemplateNotFoundException extends RuntimeException {

    public HeaderTemplateNotFoundException(long id) {
        super("Header template not found with id: " + id);
    }
}
