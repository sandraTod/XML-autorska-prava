package com.example.xml_backend.repository;

public class KorisnikRepositoryException extends RuntimeException {
    public KorisnikRepositoryException() {
        super("Baza korisnika trenutno nije dostupna.");
    }
}
