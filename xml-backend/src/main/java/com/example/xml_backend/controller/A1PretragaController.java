package com.example.xml_backend.controller;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.example.xml_backend.service.A1Service;

@RestController
public class A1PretragaController {
    private final A1Service service;

    public A1PretragaController(A1Service service) {
        this.service = service;
    }

    @GetMapping(value = "/api/a1/zavedena-dela", produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<String> searchAccepted(@RequestParam(name = "q", defaultValue = "") String q) {
        return ResponseEntity.ok().contentType(MediaType.parseMediaType("application/xml;charset=UTF-8"))
                .body(service.searchAccepted(q));
    }
}
