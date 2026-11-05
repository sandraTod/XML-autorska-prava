package com.example.xml_backend.service;

import com.example.xml_backend.model.a1pretraga.RezultatiPretrageType;
import jakarta.xml.bind.JAXBException;

public interface A1PretragaXmlService {
    RezultatiPretrageType fromXml(String xml) throws JAXBException;
    String toXml(RezultatiPretrageType rezultati) throws JAXBException;
}
