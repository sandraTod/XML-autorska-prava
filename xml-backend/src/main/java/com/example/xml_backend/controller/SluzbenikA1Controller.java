package com.example.xml_backend.controller;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.example.xml_backend.service.A1Service;

@RestController
@RequestMapping(value = "/api/sluzbenik/a1/zahtevi", produces = MediaType.APPLICATION_XML_VALUE)
public class SluzbenikA1Controller {
    private final A1Service service;

    public SluzbenikA1Controller(A1Service service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<String> findAllRequests() {
        return ResponseEntity.ok().contentType(MediaType.parseMediaType("application/xml;charset=UTF-8"))
                .body(service.findAllRequests());
    }

    @GetMapping("/pretraga")
    public ResponseEntity<String> searchPending(@RequestParam(name = "q", defaultValue = "") String q) {
        return ResponseEntity.ok().contentType(MediaType.parseMediaType("application/xml;charset=UTF-8"))
                .body(service.searchPending(q));
    }
}
