package com.example.xml_backend.service.exception;

public class InvalidPrijavaXmlException extends RuntimeException {
    public InvalidPrijavaXmlException() {
        super("Neispravan XML prijave.");
    }
}
