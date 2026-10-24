package com.example.xml_backend.service;

import com.example.xml_backend.model.prijavaodgovor.PrijavaOdgovorType;
import jakarta.xml.bind.JAXBException;

public interface PrijavaOdgovorXmlService {
    
	PrijavaOdgovorType fromXml(String xml) throws JAXBException;
    String toXml(PrijavaOdgovorType odgovor) throws JAXBException;
}
