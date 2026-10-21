package com.example.xml_backend.service;

import com.example.xml_backend.model.korisnik.KorisnikType;
import jakarta.xml.bind.JAXBException;

public interface KorisnikXmlService {
    KorisnikType fromXml(String xml) throws JAXBException;
    String toXml(KorisnikType korisnik) throws JAXBException;
}
