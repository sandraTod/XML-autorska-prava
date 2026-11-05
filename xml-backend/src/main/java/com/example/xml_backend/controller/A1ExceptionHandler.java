package com.example.xml_backend.controller;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import com.example.xml_backend.repository.A1RepositoryException;
import com.example.xml_backend.repository.KorisnikRepositoryException;
import com.example.xml_backend.repository.ResenjeAlreadyExistsException;
import com.example.xml_backend.service.exception.InvalidOdlukaXmlException;
import com.example.xml_backend.repository.ResenjeNotFoundException;
import com.example.xml_backend.service.exception.InvalidA1XmlException;
import com.example.xml_backend.service.exception.ForbiddenA1FieldsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.access.AccessDeniedException;

@RestControllerAdvice(assignableTypes = {A1Controller.class, A1PretragaController.class, SluzbenikA1Controller.class, A1PretragaController.class, ResenjeController.class})
public class A1ExceptionHandler {
	
	
    @ExceptionHandler({InvalidA1XmlException.class, HttpMessageNotReadableException.class})
    public ResponseEntity<String> invalidXml(Exception ignored) {
        return error(400, "Neispravan A-1 XML.");
    }

    @ExceptionHandler(InvalidOdlukaXmlException.class)
    public ResponseEntity<String> invalidDecision(InvalidOdlukaXmlException ignored) {
        return error(400, "Neispravna odluka XML.");
    }

    @ExceptionHandler(ResenjeNotFoundException.class)
    public ResponseEntity<String> resolutionMissing(ResenjeNotFoundException ignored) {
        return error(404, "Resurs nije pronadjen.");
    }

    @ExceptionHandler(ResenjeAlreadyExistsException.class)
    public ResponseEntity<String> alreadyDecided(ResenjeAlreadyExistsException ignored) {
        return error(409, "Odluka je vec doneta.");
    }

    @ExceptionHandler(ForbiddenA1FieldsException.class)
    public ResponseEntity<String> forbiddenFields(ForbiddenA1FieldsException ignored) {
        return error(400, "Unos polja id, vlasnikId i sluzbeniDeo nije dozvoljen.");
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<String> unauthenticated(AuthenticationException ignored) {
        return error(401, "Potrebna je važeća prijava.");
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<String> forbidden(AccessDeniedException ignored) {
        return error(403, "Pristup nije dozvoljen.");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> badRequest(Exception ignored) {
        return error(400, "Neispravan A-1 zahtev.");
    }

    @ExceptionHandler({A1RepositoryException.class, KorisnikRepositoryException.class})
    public ResponseEntity<String> unavailable(RuntimeException ignored) {
        return error(503, "Baza podataka trenutno nije dostupna.");
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<String> unsupportedMediaType(Exception ignored) {
        return error(415, "Očekivan je application/xml.");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> internalError(Exception ignored) {
        return error(500, "Interna greška pri obradi zahteva.");
    }

    static ResponseEntity<String> error(int status, String message) {
        return ResponseEntity.status(status).contentType(MediaType.parseMediaType("application/xml;charset=UTF-8"))
                .body("<?xml version=\"1.0\" encoding=\"UTF-8\"?><greska><status>"
                        + status + "</status><poruka>" + message + "</poruka></greska>");
    }
}
