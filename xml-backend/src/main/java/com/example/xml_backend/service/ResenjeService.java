package com.example.xml_backend.service;

import java.net.URI;

public interface ResenjeService {
    CreatedDocument create(String documentId, String odlukaXml);
    String findById(String documentId);

    record CreatedDocument(URI location, String xml) {}
}
