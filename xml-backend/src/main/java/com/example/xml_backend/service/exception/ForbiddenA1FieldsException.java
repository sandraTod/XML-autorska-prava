package com.example.xml_backend.service.exception;

public class ForbiddenA1FieldsException extends IllegalArgumentException {
    public ForbiddenA1FieldsException() {
        super("Unos polja id, vlasnikId i sluzbeniDeo nije dozvoljen.");
    }
}
