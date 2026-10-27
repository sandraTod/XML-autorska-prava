package com.example.xml_backend.service.impl;

import java.net.URI;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.access.AccessDeniedException;
import com.example.xml_backend.model.a1.ZahtevA1Type;
import com.example.xml_backend.repository.A1Repository;
import com.example.xml_backend.repository.A1RepositoryException;
import com.example.xml_backend.service.A1Service;
import com.example.xml_backend.service.A1XmlService;
import com.example.xml_backend.service.A1ListaXmlService;
import com.example.xml_backend.service.exception.InvalidA1XmlException;
import com.example.xml_backend.service.exception.ForbiddenA1FieldsException;
import jakarta.xml.bind.JAXBException;

@Service
public class A1ServiceImpl implements A1Service {
	
    private final A1XmlService xmlService;
    private final A1Repository repository;
    private final String documentBaseUrl;
    private final A1ListaXmlService listaXmlService;

    public A1ServiceImpl(A1XmlService xmlService, A1Repository repository, A1ListaXmlService listaXmlService,
            @Value("${a1.document-base-url}") String documentBaseUrl) {
        this.xmlService = xmlService;
        this.repository = repository;
        this.listaXmlService = listaXmlService;
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
        Authentication authentication = requireRole("ROLE_GRADJANIN");
        if (xml == null || xml.isBlank()) {
            throw new InvalidA1XmlException();
        }
        final ZahtevA1Type zahtev;
        try {
            zahtev = xmlService.fromXml(xml);
        } catch (JAXBException e) {
            throw new InvalidA1XmlException();
        }
        if (zahtev.getId() != null || zahtev.getSluzbeniDeo() != null || zahtev.getVlasnikId() != null) {
            throw new ForbiddenA1FieldsException();
        }
        String documentId = UUID.randomUUID().toString();
        URI location = URI.create(documentBaseUrl + "/" + documentId);
        zahtev.setId(location.toString());
        zahtev.setVlasnikId(authentication.getName());
        
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
       
    	Authentication authentication = requireRole("ROLE_GRADJANIN", "ROLE_SLUZBENIK");
       
    	Optional<String> stored = repository.findById(documentId);
        
    	if (stored.isEmpty()) {
            return Optional.empty();
        }
        final ZahtevA1Type zahtev;
        try {
            zahtev = xmlService.fromXml(stored.get());
        } catch (JAXBException exception) {
            throw new IllegalStateException("Neispravan sačuvani A-1 dokument");
        }
        boolean official = authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_SLUZBENIK".equals(authority.getAuthority()));
       
        return official || authentication.getName().equals(zahtev.getVlasnikId()) ? stored : Optional.empty();
    }

    @Override
    public String findMyRequests() {
        Authentication authentication = requireRole("ROLE_GRADJANIN");
        String owner = authentication.getName();
        String storedList = repository.findByOwner(owner);
        try {
            var list = listaXmlService.fromXml(storedList);
            return listaXmlService.toXml(list);
        } catch (JAXBException exception) {
            throw new A1RepositoryException("Neispravan XML odgovora baze");
        }
    }

    @Override
    public String findAllRequests() {
        requireRole("ROLE_SLUZBENIK");
        String storedList = repository.findAll();
        try {
            var list = listaXmlService.fromXml(storedList);
            return listaXmlService.toXml(list);
        } catch (JAXBException exception) {
            throw new A1RepositoryException("Neispravan XML odgovora baze");
        }
    }

    private Authentication requireRole(String... roles) {
        
    	Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    	
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken
                || authentication.getName() == null || authentication.getName().isBlank()) {
            throw new AuthenticationCredentialsNotFoundException("Potrebna je prijava");
        }
        for (String role : roles) {
            if (authentication.getAuthorities().stream().anyMatch(authority -> role.equals(authority.getAuthority()))) {
                return authentication;
            }
        }
        throw new AccessDeniedException("Pristup nije dozvoljen");
    }
}
