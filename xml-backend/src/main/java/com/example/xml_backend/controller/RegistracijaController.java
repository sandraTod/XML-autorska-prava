package com.example.xml_backend.controller;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import com.example.xml_backend.service.RegistracijaService;

@RestController
public class RegistracijaController {
    private final RegistracijaService service;

    public RegistracijaController(RegistracijaService service) {
        this.service = service;
    }

    @PostMapping(value = "/api/auth/registracija", consumes = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<Void> register(@RequestBody String xml) {
        service.register(xml);
        return ResponseEntity.status(201).build();
    }
}
