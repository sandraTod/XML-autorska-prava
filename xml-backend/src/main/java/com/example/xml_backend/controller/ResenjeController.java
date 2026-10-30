package com.example.xml_backend.controller;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import com.example.xml_backend.service.ResenjeService;

@RestController
public class ResenjeController {
    private final ResenjeService service;

    public ResenjeController(ResenjeService service) {
        this.service = service;
    }

    @PostMapping(value = "/api/sluzbenik/a1/zahtevi/{documentId}/resenje",
            consumes = MediaType.APPLICATION_XML_VALUE, produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<String> create(@PathVariable String documentId, @RequestBody String xml) {
        ResenjeService.CreatedDocument created = service.create(documentId, xml);
        return ResponseEntity.created(created.location()).contentType(MediaType.APPLICATION_XML).body(created.xml());
    }

    @GetMapping(value = "/api/a1/zahtevi/{documentId}/resenje", produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<String> findById(@PathVariable String documentId) {
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_XML).body(service.findById(documentId));
    }
}
