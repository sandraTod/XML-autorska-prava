package com.example.xml_backend.repository;

public class ResenjeAlreadyExistsException extends RuntimeException {
    public ResenjeAlreadyExistsException() {
        super("Odluka je vec doneta.");
    }
}
