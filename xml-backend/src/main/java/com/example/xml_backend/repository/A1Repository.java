package com.example.xml_backend.repository;

import java.util.Optional;

public interface A1Repository {
	
    void save(String documentId, String xml);
    
    Optional<String> findById(String documentId);
}
