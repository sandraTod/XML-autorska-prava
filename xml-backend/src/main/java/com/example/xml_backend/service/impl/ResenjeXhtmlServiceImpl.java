package com.example.xml_backend.service.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;

import javax.xml.XMLConstants;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.springframework.stereotype.Service;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import com.example.xml_backend.service.ResenjeXhtmlService;

@Service
public class ResenjeXhtmlServiceImpl implements ResenjeXhtmlService {
    private final Templates template;

    public ResenjeXhtmlServiceImpl() {
        try (InputStream input = ResenjeXhtmlServiceImpl.class.getResourceAsStream("/xslt/resenje-xhtml.xsl")) {
            if (input == null) {
                throw new IllegalStateException("Nedostaje XHTML rešenja šablon");
            }
            TransformerFactory factory = TransformerFactory.newDefaultInstance();
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
            factory.setURIResolver((href, base) -> {
                throw new TransformerException("Spoljni resursi nisu dozvoljeni");
            });
            template = factory.newTemplates(new StreamSource(input));
        } catch (IOException | TransformerException e) {
            throw new IllegalStateException("Neuspešna inicijalizacija XHTML rešenja servisa", e);
        }
    }

    @Override
    public String toXhtml(String xml) {
        try {
            Transformer transformer = template.newTransformer();
            transformer.setURIResolver((href, base) -> {
                throw new TransformerException("Spoljni resursi nisu dozvoljeni");
            });
            SAXSource source = new SAXSource(newXmlReader(), new InputSource(new StringReader(xml)));
            StringWriter output = new StringWriter();
            transformer.transform(source, new StreamResult(output));
            return output.toString();
        } catch (ParserConfigurationException | SAXException | TransformerException e) {
            throw new IllegalStateException("Neuspešna XHTML rešenja transformacija", e);
        }
    }

    private XMLReader newXmlReader() throws ParserConfigurationException, SAXException {
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
    }
}
