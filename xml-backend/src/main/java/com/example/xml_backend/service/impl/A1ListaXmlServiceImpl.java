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
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.springframework.stereotype.Service;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import com.example.xml_backend.model.a1.ObjectFactory;
import com.example.xml_backend.model.a1.ListaZahtevaType;
import com.example.xml_backend.service.A1ListaXmlService;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;

@Service
public class A1ListaXmlServiceImpl implements A1ListaXmlService {

    private final JAXBContext context;
    private final Schema schema;

    public A1ListaXmlServiceImpl() {
        try (InputStream input = A1ListaXmlServiceImpl.class.getResourceAsStream("/a1-lista.xsd")) {
            if (input == null) {
                throw new IllegalStateException("Nedostaje classpath resurs a1-lista.xsd");
            }
            SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            factory.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            try (InputStream a1Input = A1ListaXmlServiceImpl.class.getResourceAsStream("/a1-zahtev.xsd")) {
                if (a1Input == null) { throw new IllegalStateException("Nedostaje a1-zahtev.xsd"); }
                // Uvezena šema se unapred učitava samo sa classpath-a; spoljašnji pristup ostaje zabranjen.
                schema = factory.newSchema(new Source[]{new StreamSource(a1Input), new StreamSource(input)});
            }
            context = JAXBContext.newInstance(ObjectFactory.class);
        } catch (IOException | SAXException | JAXBException e) {
            throw new IllegalStateException("Neuspešna inicijalizacija A-1 XML servisa", e);
        }
    }

    
    @Override
    public ListaZahtevaType fromXml(String xml) throws JAXBException {
        Objects.requireNonNull(xml, "xml");
        Unmarshaller unmarshaller = context.createUnmarshaller();
        unmarshaller.setSchema(schema);
        unmarshaller.setEventHandler(event -> false);
        SAXSource source = new SAXSource(newXmlReader(), new InputSource(new StringReader(xml)));
        JAXBElement<ListaZahtevaType> root = unmarshaller.unmarshal(source, ListaZahtevaType.class);
        return root.getValue();
    }

    
    @Override
    public String toXml(ListaZahtevaType lista) throws JAXBException {
        Objects.requireNonNull(lista, "lista");
        Marshaller marshaller = context.createMarshaller();
        marshaller.setSchema(schema);
        marshaller.setEventHandler(event -> false);
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        marshaller.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");
        StringWriter output = new StringWriter();
        marshaller.marshal(new ObjectFactory().createListaZahteva(lista), output);
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
