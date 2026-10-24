package com.example.xml_backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import com.example.xml_backend.repository.KorisnikRepositoryException;
import com.example.xml_backend.service.exception.InvalidCredentialsException;
import com.example.xml_backend.service.exception.InvalidPrijavaXmlException;

@RestControllerAdvice(assignableTypes = PrijavaController.class)
public class PrijavaExceptionHandler {
    @ExceptionHandler({InvalidPrijavaXmlException.class, HttpMessageNotReadableException.class})
    public ResponseEntity<String> invalidXml(Exception ignored) {
        return error(400, "Neispravan XML prijave.");
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<String> invalidCredentials(InvalidCredentialsException ignored) {
        return error(401, "Neispravan email ili lozinka.");
    }

    @ExceptionHandler(KorisnikRepositoryException.class)
    public ResponseEntity<String> unavailable(KorisnikRepositoryException ignored) {
        return error(503, "Baza podataka trenutno nije dostupna.");
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<String> unsupported(Exception ignored) {
        return error(415, "Očekivan je application/xml.");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> unexpected(Exception ignored) {
        return error(500, "Interna greška pri prijavi.");
    }

    private ResponseEntity<String> error(int status, String message) {
        var response = A1ExceptionHandler.error(status, message);
        return ResponseEntity.status(status).headers(response.getHeaders())
                .header("Cache-Control", "no-store").body(response.getBody());
    }
}
