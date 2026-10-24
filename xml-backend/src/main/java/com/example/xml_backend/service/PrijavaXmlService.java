package com.example.xml_backend.service;

import com.example.xml_backend.model.prijava.PrijavaType;
import jakarta.xml.bind.JAXBException;

public interface PrijavaXmlService {
    
	PrijavaType fromXml(String xml) throws JAXBException;
    String toXml(PrijavaType prijava) throws JAXBException;
}
