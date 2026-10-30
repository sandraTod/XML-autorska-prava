package com.example.xml_backend.service;

import com.example.xml_backend.model.odluka.OdlukaType;
import jakarta.xml.bind.JAXBException;

public interface OdlukaXmlService {
    OdlukaType fromXml(String xml) throws JAXBException;
    String toXml(OdlukaType odluka) throws JAXBException;
}
