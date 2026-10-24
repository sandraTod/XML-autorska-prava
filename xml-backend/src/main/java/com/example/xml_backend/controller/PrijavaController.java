package com.example.xml_backend.controller;

import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import com.example.xml_backend.service.PrijavaService;

@RestController
public class PrijavaController {
    private final PrijavaService service;

    public PrijavaController(PrijavaService service) {
        this.service = service;
    }

    @PostMapping(value = "/api/auth/prijava", consumes = MediaType.APPLICATION_XML_VALUE,
            produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<String> login(@RequestBody String xml) {
        return ResponseEntity.ok().contentType(MediaType.parseMediaType("application/xml;charset=UTF-8"))
                .cacheControl(CacheControl.noStore()).body(service.login(xml));
    }
}
