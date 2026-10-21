package com.example.xml_backend.repository;

public class EmailAlreadyExistsException extends RuntimeException {
    public EmailAlreadyExistsException() {
        super("E-mail je već registrovan.");
    }
}
