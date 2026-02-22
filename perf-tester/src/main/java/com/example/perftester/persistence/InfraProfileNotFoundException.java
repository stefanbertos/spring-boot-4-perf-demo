package com.example.perftester.persistence;

public class InfraProfileNotFoundException extends RuntimeException {

    public InfraProfileNotFoundException(long id) {
        super("Infra profile not found with id: " + id);
    }
}
