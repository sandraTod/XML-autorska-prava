package com.example.xml_backend.service;

import com.example.xml_backend.model.registracija.RegistracijaType;
import jakarta.xml.bind.JAXBException;

public interface RegistracijaXmlService {
    RegistracijaType fromXml(String xml) throws JAXBException;
    String toXml(RegistracijaType registracija) throws JAXBException;
}
