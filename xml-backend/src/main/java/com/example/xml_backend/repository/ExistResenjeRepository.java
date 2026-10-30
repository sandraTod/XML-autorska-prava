package com.example.xml_backend.repository;

import java.io.IOException;
import javax.xml.stream.XMLOutputFactory;
import java.io.StringWriter;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.Objects;
import java.util.Optional;
import java.util.Map;
import java.util.regex.Pattern;
import javax.xml.stream.XMLStreamException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

@Repository
public class ExistResenjeRepository implements ResenjeRepository {
	
    private static final String COLLECTION = "/db/autorska-prava/resenja/";
    private static final Pattern SAFE_ID = Pattern.compile("[A-Za-z0-9][A-Za-z0-9_-]{0,99}");
    private final URI baseUri;
    private final String username;
    private final String password;
    private final Duration requestTimeout;
    private final HttpClient client;
    private final String saveQuery = loadQuery("resenje-save.xq");

    public ExistResenjeRepository(
            @Value("${exist-db.url}") String url,
            @Value("${exist-db.username}") String username,
            @Value("${exist-db.password}") String password,
            @Value("${exist-db.connect-timeout-seconds}") long connectTimeoutSeconds,
            @Value("${exist-db.request-timeout-seconds}") long requestTimeoutSeconds) {
        baseUri = URI.create(url.replaceAll("/+$", ""));
        if (!("http".equals(baseUri.getScheme()) || "https".equals(baseUri.getScheme()))
                || baseUri.getHost() == null || baseUri.getUserInfo() != null
                || baseUri.getQuery() != null || baseUri.getFragment() != null) {
            throw new IllegalArgumentException("Neispravan eXist REST URL");
        }
        if (username == null || username.isBlank() || username.contains(":")) {
            throw new IllegalArgumentException("Neispravno eXist korisničko ime");
        }
        if (connectTimeoutSeconds <= 0 || requestTimeoutSeconds <= 0) {
            throw new IllegalArgumentException("HTTP timeout mora biti pozitivan");
        }
        this.username = username;
        this.password = Objects.requireNonNull(password);
        requestTimeout = Duration.ofSeconds(requestTimeoutSeconds);
        client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(connectTimeoutSeconds))
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
    }

    @Override
    public void saveIfAbsent(String documentId, String xml) {
        documentUri(documentId);
        String response = execute(saveQuery, Map.of("documentId", documentId, "xml", xml)).strip();
        // The query returns only one fixed, empty marker, never document data.
        if (response.matches("(?s)(<\\?xml[^?]*\\?>\\s*)?<duplicate\\s*/>")) {
            throw new ResenjeAlreadyExistsException();
        }
        if (response.matches("(?s)(<\\?xml[^?]*\\?>\\s*)?<missing\\s*/>")) {
            throw new ResenjeNotFoundException();
        }
        if (!response.matches("(?s)(<\\?xml[^?]*\\?>\\s*)?<created\\s*/>")) {
            throw new A1RepositoryException("Neispravan odgovor baze pri cuvanju resenja");
        }
    }

    @Override
    public Optional<String> findById(String documentId) {
        URI uri = URI.create(documentUri(documentId) + "?_xsl=no&_encoding=UTF-8");
        HttpResponse<String> response = send(request(uri).GET().build());
        if (response.statusCode() == 404) {
            return Optional.empty();
        }
        if (response.statusCode() != 200) {
            throw httpFailure("čitanje", response.statusCode());
        }
        return Optional.of(response.body());
    }

    private String execute(String query, Map<String, String> variables) {
        final String body;
        try {
            body = queryBody(query, variables);
        } catch (XMLStreamException exception) {
            throw new A1RepositoryException("Neuspešna priprema upita");
        }
        var request = request(URI.create(baseUri + "/db"))
                .header("Content-Type", "application/xml; charset=UTF-8")
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8)).build();
        var response = send(request);
        if (response.statusCode() != 200) {
            throw httpFailure("čitanje liste", response.statusCode());
        }
        return response.body();
    }

    private static String loadQuery(String filename) {
        try (var input = ExistResenjeRepository.class.getResourceAsStream("/exist/" + filename)) {
            if (input == null) {
                throw new IllegalStateException("Nedostaje upit za resenje");
            }
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("Nije moguće učitati upit za resenje");
        }
    }

    private URI documentUri(String documentId) {
        if (documentId == null || !SAFE_ID.matcher(documentId).matches()) {
            throw new IllegalArgumentException("documentId: 1–100 slova, cifara, _ ili -; prvo slovo ili cifra, bez .xml");
        }
        return URI.create(baseUri + COLLECTION + documentId + ".xml");
    }

    private HttpRequest.Builder request(URI uri) {
        if (password.isEmpty()) {
            throw new A1RepositoryException("Postavi environment promenljivu EXIST_DB_PASSWORD");
        }
        String credentials = Base64.getEncoder().encodeToString(
                (username + ":" + password).getBytes(StandardCharsets.UTF_8));
        return HttpRequest.newBuilder(uri).timeout(requestTimeout)
                .header("Accept", "application/xml")
                .header("Authorization", "Basic " + credentials);
    }

    private HttpResponse<String> send(HttpRequest request) {
        try {
            return client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new A1RepositoryException("eXist HTTP operacija je prekinuta");
        } catch (IOException e) {
            throw new A1RepositoryException("eXist nije dostupan ili je HTTP timeout istekao");
        }
    }

    private A1RepositoryException httpFailure(String operation, int status) {
        return new A1RepositoryException("eXist " + operation + " nije uspelo (HTTP " + status + ")");
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
