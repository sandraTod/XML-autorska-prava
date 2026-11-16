package com.example.xml_backend.service.impl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.net.URI;

import javax.xml.XMLConstants;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamSource;

import org.apache.fop.apps.Fop;
import org.apache.fop.apps.FopConfParser;
import org.apache.fop.apps.FopFactory;
import org.apache.xmlgraphics.io.Resource;
import org.apache.xmlgraphics.io.ResourceResolver;
import org.apache.xmlgraphics.util.MimeConstants;
import org.springframework.stereotype.Service;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import com.example.xml_backend.service.ResenjePdfService;

@Service
public class ResenjePdfServiceImpl implements ResenjePdfService {
    private final Templates template;
    private final FopFactory fopFactory;

    public ResenjePdfServiceImpl() {
        try (InputStream input = ResenjePdfServiceImpl.class.getResourceAsStream("/xslt/resenje-fo.xsl");
                InputStream config = ResenjePdfServiceImpl.class.getResourceAsStream("/fop/resenje-fop.xconf")) {
            if (input == null || config == null) {
                throw new IllegalStateException("Nedostaje PDF šablon ili FOP konfiguracija rešenja");
            }
            TransformerFactory factory = TransformerFactory.newDefaultInstance();
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
            factory.setURIResolver((href, base) -> {
                throw new TransformerException("Spoljni resursi nisu dozvoljeni");
            });
            template = factory.newTemplates(new StreamSource(input));
            ResourceResolver resources = new ResourceResolver() {
                @Override
                public Resource getResource(URI uri) throws IOException {
                    if (!URI.create("classpath:/fonts/DejaVuSans.ttf").equals(uri)) {
                        throw new IOException("Resurs nije dozvoljen");
                    }
                    InputStream font = ResenjePdfServiceImpl.class.getResourceAsStream("/fonts/DejaVuSans.ttf");
                    if (font == null) {
                        throw new IOException("Nedostaje PDF font");
                    }
                    return new Resource(font);
                }

                @Override
                public OutputStream getOutputStream(URI uri) throws IOException {
                    throw new IOException("Upis resursa nije dozvoljen");
                }
            };
            fopFactory = new FopConfParser(config, URI.create("classpath:/"), resources)
                    .getFopFactoryBuilder().build();
        } catch (IOException | SAXException | TransformerException e) {
            throw new IllegalStateException("Neuspešna inicijalizacija PDF servisa rešenja", e);
        }
    }

    @Override
    public byte[] toPdf(String xml) {
        try {
            Transformer transformer = template.newTransformer();
            transformer.setURIResolver((href, base) -> {
                throw new TransformerException("Spoljni resursi nisu dozvoljeni");
            });
            SAXSource source = new SAXSource(newXmlReader(), new InputSource(new StringReader(xml)));
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            Fop fop = fopFactory.newFop(MimeConstants.MIME_PDF, output);
            transformer.transform(source, new SAXResult(fop.getDefaultHandler()));
            return output.toByteArray();
        } catch (ParserConfigurationException | SAXException | TransformerException e) {
            throw new IllegalStateException("Neuspešna PDF transformacija rešenja", e);
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
