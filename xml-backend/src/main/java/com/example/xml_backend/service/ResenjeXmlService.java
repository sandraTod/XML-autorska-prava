package com.example.xml_backend.service;

import com.example.xml_backend.model.resenje.ResenjeType;
import jakarta.xml.bind.JAXBException;

public interface ResenjeXmlService {
    ResenjeType fromXml(String xml) throws JAXBException;
    String toXml(ResenjeType resenje) throws JAXBException;
}
