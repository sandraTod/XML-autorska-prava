package com.example.xml_backend.service.impl;

import java.net.URI;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.example.xml_backend.repository.A1Repository;
import com.example.xml_backend.service.A1Service;
import com.example.xml_backend.service.A1XmlService;
import com.example.xml_backend.service.exception.InvalidA1XmlException;
import com.example.xml_backend.service.exception.ForbiddenA1FieldsException;
import jakarta.xml.bind.JAXBException;

@Service
public class A1ServiceImpl implements A1Service {
	
    private final A1XmlService xmlService;
    private final A1Repository repository;
    private final String documentBaseUrl;

    public A1ServiceImpl(A1XmlService xmlService, A1Repository repository,
            @Value("${a1.document-base-url}") String documentBaseUrl) {
        this.xmlService = xmlService;
        this.repository = repository;
        URI base = URI.create(documentBaseUrl);
        if (!("http".equals(base.getScheme()) || "https".equals(base.getScheme()))
                || base.getHost() == null || base.getUserInfo() != null
                || base.getQuery() != null || base.getFragment() != null) {
            throw new IllegalArgumentException("Neispravna osnova URL identifikatora");
        }
        this.documentBaseUrl = documentBaseUrl.replaceAll("/+$", "");
    }

    @Override
    public CreatedDocument create(String xml) {
    	
        if (xml == null || xml.isBlank()) {
            throw new InvalidA1XmlException();
        }
        final com.example.xml_backend.model.a1.ZahtevA1Type zahtev;
        try {
            zahtev = xmlService.fromXml(xml);
        } catch (JAXBException e) {
            throw new InvalidA1XmlException();
        }
        if (zahtev.getId() != null || zahtev.getSluzbeniDeo() != null) {
            throw new ForbiddenA1FieldsException();
        }
        String documentId = UUID.randomUUID().toString();
        URI location = URI.create(documentBaseUrl + "/" + documentId);
        zahtev.setId(location.toString());
        
        final String savedXml;
        
        try {
            savedXml = xmlService.toXml(zahtev);
        } catch (JAXBException e) {
            throw new IllegalStateException("Neuspešno generisanje A-1 XML-a");
        }
        repository.save(documentId, savedXml);
        return new CreatedDocument(location, savedXml);
    }

    @Override
    public Optional<String> findById(String documentId) {
        return repository.findById(documentId);
    }
}
