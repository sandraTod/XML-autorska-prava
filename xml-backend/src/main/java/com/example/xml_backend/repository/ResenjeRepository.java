package com.example.xml_backend.repository;

import java.util.Optional;

public interface ResenjeRepository {
    
	void saveIfAbsent(String documentId, String xml);
    
	Optional<String> findById(String documentId);
}
