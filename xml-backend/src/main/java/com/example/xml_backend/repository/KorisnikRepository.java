package com.example.xml_backend.repository;

import java.util.Optional;

public interface KorisnikRepository {
	
    
    void save(String email, String korisnikXml);
    
    Optional<String> findByEmail(String email);

    Optional<String> findById(String id);
}
