package com.example.xml_backend.service.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Objects;

import javax.xml.XMLConstants;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.springframework.stereotype.Service;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import com.example.xml_backend.model.prijava.ObjectFactory;
import com.example.xml_backend.model.prijava.PrijavaType;
import com.example.xml_backend.service.PrijavaXmlService;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;

@Service
public class PrijavaXmlServiceImpl implements PrijavaXmlService {

    private final JAXBContext context;
    private final Schema schema;

    public PrijavaXmlServiceImpl() {
        try (InputStream input = PrijavaXmlServiceImpl.class.getResourceAsStream("/prijava.xsd")) {
            if (input == null) {
                throw new IllegalStateException("Nedostaje classpath resurs prijava.xsd");
            }
            SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            factory.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            schema = factory.newSchema(new StreamSource(input));
            context = JAXBContext.newInstance(ObjectFactory.class);
        } catch (IOException | SAXException | JAXBException e) {
            throw new IllegalStateException("Neuspešna inicijalizacija Registracija XML servisa", e);
        }
    }

    
    @Override
    public PrijavaType fromXml(String xml) throws JAXBException {
        Objects.requireNonNull(xml, "xml");
        Unmarshaller unmarshaller = context.createUnmarshaller();
        unmarshaller.setSchema(schema);
        unmarshaller.setEventHandler(event -> false);
        SAXSource source = new SAXSource(newXmlReader(), new InputSource(new StringReader(xml)));
        JAXBElement<PrijavaType> root = unmarshaller.unmarshal(source, PrijavaType.class);
        return root.getValue();
    }

    
    @Override
    public String toXml(PrijavaType prijava) throws JAXBException {
        Objects.requireNonNull(prijava, "prijava");
        Marshaller marshaller = context.createMarshaller();
        marshaller.setSchema(schema);
        marshaller.setEventHandler(event -> false);
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        marshaller.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");
        StringWriter output = new StringWriter();
        marshaller.marshal(new ObjectFactory().createPrijava(prijava), output);
        return output.toString();
    }

    private XMLReader newXmlReader() throws JAXBException {
        try {
            SAXParserFactory factory = SAXParserFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setXIncludeAware(false);
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            XMLReader reader = factory.newSAXParser().getXMLReader();
            reader.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            reader.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            reader.setEntityResolver((publicId, systemId) -> {
                throw new SAXException("Spoljni entiteti nisu dozvoljeni");
            });
            return reader;
        } catch (ParserConfigurationException | SAXException e) {
            throw new JAXBException("Neuspešno podešavanje bezbednog XML parsera", e);
        }
    }
}
