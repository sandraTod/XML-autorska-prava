package com.example.xml_backend.controller;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.example.xml_backend.service.A1Service;
import com.example.xml_backend.service.A1XhtmlService;

@RestController
@RequestMapping(value = "/api/a1/zahtevi", produces = MediaType.APPLICATION_XML_VALUE)
public class A1Controller {
	
    private static final MediaType XML_UTF8 = MediaType.parseMediaType("application/xml;charset=UTF-8");
    
    private final A1Service service;
    private final A1XhtmlService xhtmlService;

    public A1Controller(A1Service service, A1XhtmlService xhtmlService) {
        this.service = service;
        this.xhtmlService = xhtmlService;
    }

    @PostMapping(consumes = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<String> create(@RequestBody String xml) {
    	var created = service.create(xml);
        return ResponseEntity.created(created.location()).contentType(XML_UTF8)
                .body(created.xml());
    }

    @GetMapping("/{documentId}")
    public ResponseEntity<String> findById(@PathVariable String documentId) {
        return service.findById(documentId)
                .map(xml -> ResponseEntity.ok().contentType(XML_UTF8).body(xml))
                .orElseGet(() -> A1ExceptionHandler.error(404, "Zahtev nije pronađen."));
    }

    @GetMapping(value = "/{documentId}/xhtml", produces = "application/xhtml+xml")
    public ResponseEntity<String> findXhtml(@PathVariable String documentId) {
        return service.findById(documentId)
                .map(xml -> ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType("application/xhtml+xml;charset=UTF-8"))
                        .body(xhtmlService.toXhtml(xml)))
                .orElseGet(() -> A1ExceptionHandler.error(404, "Zahtev nije pronađen."));
    }

    @GetMapping("/listaZahteva")
    public ResponseEntity<String> findMyRequests(@RequestParam(name = "vlasnikId", required = false) String requestedOwner) {
        if (requestedOwner != null) {
            throw new IllegalArgumentException("Vlasnik se ne zadaje kroz zahtev");
        }
        return ResponseEntity.ok().contentType(XML_UTF8).body(service.findMyRequests());
    }
}
