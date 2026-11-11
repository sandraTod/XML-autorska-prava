package com.example.xml_backend.controller;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import com.example.xml_backend.service.ResenjeService;
import com.example.xml_backend.service.ResenjeXhtmlService;

@RestController
public class ResenjeController {
    private final ResenjeService service;
    private final ResenjeXhtmlService xhtmlService;

    public ResenjeController(ResenjeService service, ResenjeXhtmlService xhtmlService) {
        this.service = service;
        this.xhtmlService = xhtmlService;
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

    @GetMapping(value = "/api/a1/zahtevi/{documentId}/resenje/xhtml", produces = "application/xhtml+xml")
    public ResponseEntity<String> findXhtml(@PathVariable String documentId) {
        String xml = service.findById(documentId);
        return ResponseEntity.ok().contentType(MediaType.parseMediaType("application/xhtml+xml;charset=UTF-8"))
                .body(xhtmlService.toXhtml(xml));
    }
}
