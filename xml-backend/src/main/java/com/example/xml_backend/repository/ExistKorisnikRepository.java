package com.example.xml_backend.repository;

import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import javax.xml.stream.XMLOutputFactory;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import com.example.xml_backend.service.EmailNormalizer;

@Repository
public class ExistKorisnikRepository implements KorisnikRepository {
	
    private final URI endpoint;
    private final String authorization;
    private final Duration timeout;
    private final HttpClient client;
    private final String saveQuery = loadQuery("korisnik-save.xq");
    private final String findQuery = loadQuery("korisnik-find.xq");

    public ExistKorisnikRepository(@Value("${exist-db.url}") String url,
            @Value("${exist-db.username}") String username,
            @Value("${exist-db.password}") String password,
            @Value("${exist-db.connect-timeout-seconds}") long connectSeconds,
            @Value("${exist-db.request-timeout-seconds}") long requestSeconds) {
        URI base = URI.create(url.replaceAll("/+$", ""));
        
        if (!("http".equals(base.getScheme()) || "https".equals(base.getScheme()))
                || base.getHost() == null || base.getUserInfo() != null
                || base.getQuery() != null || base.getFragment() != null
                || username == null || username.isBlank() || username.contains(":")) {
            throw new IllegalArgumentException("Neispravna konfiguracija baze korisnika");
        }
        endpoint = URI.create(base + "/db");
        authorization = password.isEmpty() ? null : "Basic " + Base64.getEncoder()
                .encodeToString((username + ":" + password).getBytes(StandardCharsets.UTF_8));
        timeout = Duration.ofSeconds(requestSeconds);
        if (timeout.isNegative() || timeout.isZero()) {
            throw new IllegalArgumentException("Timeout mora biti pozitivan");
        }
        client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(connectSeconds))
                .followRedirects(HttpClient.Redirect.NEVER).build();
    }

    @Override
    public void save(String email, String korisnikXml) {
        Document response = execute(saveQuery, Map.of("email", EmailNormalizer.normalize(email), "xml", korisnikXml));
        if (response.getElementsByTagName("duplicate").getLength() == 1) {
            throw new EmailAlreadyExistsException();
        }
        if (response.getElementsByTagName("created").getLength() != 1) {
            throw new KorisnikRepositoryException();
        }
    }

    @Override
    public Optional<String> findByEmail(String email) {
        Document response = execute(findQuery, Map.of("email", EmailNormalizer.normalize(email)));
        
        if (response.getElementsByTagName("missing").getLength() == 1) {
            return Optional.empty();
        }
        var users = response.getElementsByTagNameNS("https://example.org/autorska-prava/korisnik", "korisnik");
        
        if (users.getLength() != 1) {
            throw new KorisnikRepositoryException();
        }
        try {
            TransformerFactory factory = TransformerFactory.newInstance();
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
            StringWriter output = new StringWriter();
            factory.newTransformer().transform(new DOMSource(users.item(0)), new StreamResult(output));
            return Optional.of(output.toString());
        } catch (Exception exception) {
            throw new KorisnikRepositoryException();
        }
    }

    private Document execute(String query, Map<String, String> variables) {
       
    	if (authorization == null) {
            throw new KorisnikRepositoryException();
        }
        try {
            HttpRequest request = HttpRequest.newBuilder(endpoint).timeout(timeout)
                    .header("Authorization", authorization)
                    .header("Content-Type", "application/xml; charset=UTF-8")
                    .POST(HttpRequest.BodyPublishers.ofString(queryBody(query, variables), StandardCharsets.UTF_8)).build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() != 200) {
                throw new KorisnikRepositoryException();
            }
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            var builder = factory.newDocumentBuilder();
            builder.setErrorHandler(new org.xml.sax.helpers.DefaultHandler());
            return builder.parse(new InputSource(new StringReader(response.body())));
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new KorisnikRepositoryException();
        } catch (Exception exception) {
            // Ne prenosimo bazni odgovor, hash, lozinku ili XML parametre u logove/izuzetke.
            throw new KorisnikRepositoryException();
        }
    }

    private static String loadQuery(String filename) {
        try (var input = ExistKorisnikRepository.class.getResourceAsStream("/exist/" + filename)) {
            if (input == null) {
                throw new IllegalStateException("Nedostaje XQuery resurs");
            }
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("Nije moguće učitati XQuery resurs");
        }
    }
    private String queryBody(String query, Map<String, String> variables) throws XMLStreamException {
        StringWriter output = new StringWriter();
        var writer = XMLOutputFactory.newFactory().createXMLStreamWriter(output);
        writer.writeStartElement("query");
        writer.writeDefaultNamespace("http://exist.sourceforge.net/NS/exist");
        writer.writeAttribute("wrap", "no");
        writer.writeStartElement("text");
        writer.writeCharacters(query);
        writer.writeEndElement();
        writer.writeStartElement("variables");
        for (Map.Entry<String, String> variable : variables.entrySet()) {
            writer.writeStartElement("variable");
            writer.writeStartElement("qname");
            writer.writeStartElement("localname");
            writer.writeCharacters(variable.getKey());
            writer.writeEndElement();
            writer.writeEndElement();
            writer.writeStartElement("s", "sequence", "http://exist-db.org/xquery/types/serialized");
            writer.writeNamespace("s", "http://exist-db.org/xquery/types/serialized");
            writer.writeAttribute("item-type", "xs:string");
            writer.writeStartElement("s", "value", "http://exist-db.org/xquery/types/serialized");
            writer.writeAttribute("type", "xs:string");
            writer.writeCharacters(variable.getValue());
            writer.writeEndElement();
            writer.writeEndElement();
            writer.writeEndElement();
        }
        writer.writeEndElement();
        writer.writeEndElement();
        writer.close();
        return output.toString();
    }
}
