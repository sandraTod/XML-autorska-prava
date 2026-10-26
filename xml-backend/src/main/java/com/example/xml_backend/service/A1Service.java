package com.example.xml_backend.service;

import java.net.URI;
import java.util.Optional;

public interface A1Service {
	
	
    CreatedDocument create(String xml);
    Optional<String> findById(String documentId);
    String findMyRequests();

    record CreatedDocument(URI location, String xml) {}
}
