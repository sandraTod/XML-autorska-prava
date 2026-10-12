package com.example.xml_backend.service;

import com.example.xml_backend.model.a1.ZahtevA1Type;

import jakarta.xml.bind.JAXBException;

public interface A1XmlService {

    /** XML -> Java, uz XSD validaciju i zabranu DTD-a i spoljnih entiteta. */
    ZahtevA1Type fromXml(String xml) throws JAXBException;

    /** Java -> XML; neispravan objekat se odbija XSD validacijom. */
    String toXml(ZahtevA1Type zahtev) throws JAXBException;
}
