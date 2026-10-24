package com.example.xml_backend.service.exception;

public class InvalidCredentialsException extends RuntimeException {
    public InvalidCredentialsException() {
        super("Neispravan email ili lozinka.");
    }
}
