package com.example.xml_backend.service;

import com.example.xml_backend.model.a1.ListaZahtevaType;
import jakarta.xml.bind.JAXBException;

public interface A1ListaXmlService {
    ListaZahtevaType fromXml(String xml) throws JAXBException;
    String toXml(ListaZahtevaType lista) throws JAXBException;
}
