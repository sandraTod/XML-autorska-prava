package com.example.xml_backend.repository;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

@Repository
public class ExistA1Repository implements A1Repository {
	
    private static final String COLLECTION = "/db/autorska-prava/a1/";
    private static final Pattern SAFE_ID = Pattern.compile("[A-Za-z0-9][A-Za-z0-9_-]{0,99}");
    private final URI baseUri;
    private final String username;
    private final String password;
    private final Duration requestTimeout;
    private final HttpClient client;

    public ExistA1Repository(
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
    public void save(String documentId, String xml) {
        URI uri = documentUri(documentId);
        if (xml == null || xml.isBlank()) {
            throw new IllegalArgumentException("XML ne sme biti prazan");
        }
        HttpRequest request = request(uri)
                .header("Content-Type", "application/xml; charset=UTF-8")
                .PUT(HttpRequest.BodyPublishers.ofString(xml, StandardCharsets.UTF_8)).build();
        HttpResponse<String> response = send(request);
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw httpFailure("čuvanje", response.statusCode());
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

    private URI documentUri(String documentId) {
        if (documentId == null || !SAFE_ID.matcher(documentId).matches()) {
            throw new IllegalArgumentException("documentId: 1–100 slova, cifara, _ ili -; prvo slovo ili cifra, bez .xml");
        }
        return URI.create(baseUri + COLLECTION + documentId + ".xml");
    }

    private HttpRequest.Builder request(URI uri) {
        // Provera tek pri operaciji omogućava pokretanje aplikacije i testova bez baze.
        if (password.isEmpty()) {
            throw new IllegalStateException("Postavi environment promenljivu EXIST_DB_PASSWORD");
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
            throw new IllegalStateException("eXist HTTP operacija je prekinuta");
        } catch (IOException e) {
            // Ne uključujemo zaglavlja, telo odgovora ili kredencijale u poruku greške.
            throw new IllegalStateException("eXist nije dostupan ili je HTTP timeout istekao");
        }
    }

    private IllegalStateException httpFailure(String operation, int status) {
        return new IllegalStateException("eXist " + operation + " nije uspelo (HTTP " + status + ")");
    }
}
