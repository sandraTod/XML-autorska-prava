package com.example.xml_backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import com.example.xml_backend.repository.EmailAlreadyExistsException;
import com.example.xml_backend.repository.KorisnikRepositoryException;

@RestControllerAdvice(assignableTypes = RegistracijaController.class)
public class RegistracijaExceptionHandler {
    @ExceptionHandler({IllegalArgumentException.class, HttpMessageNotReadableException.class})
    public ResponseEntity<String> invalidRegistration(Exception ignored) {
        return A1ExceptionHandler.error(400, "Neispravna registracija: proverite XML, e-mail i dužinu lozinke.");
    }

    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ResponseEntity<String> duplicateEmail(EmailAlreadyExistsException ignored) {
        return A1ExceptionHandler.error(409, "E-mail je već registrovan.");
    }

    @ExceptionHandler(KorisnikRepositoryException.class)
    public ResponseEntity<String> unavailable(KorisnikRepositoryException ignored) {
        return A1ExceptionHandler.error(503, "Baza podataka trenutno nije dostupna.");
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<String> unsupported(Exception ignored) {
        return A1ExceptionHandler.error(415, "Očekivan je application/xml.");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> unexpected(Exception ignored) {
        return A1ExceptionHandler.error(500, "Interna greška pri registraciji.");
    }
}
