package com.example.xml_backend.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import javax.xml.parsers.DocumentBuilderFactory;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ExistKorisnikRepositoryTest {
    private HttpServer server;
    private ExistKorisnikRepository repository;
    private final AtomicReference<String> responseBody = new AtomicReference<>("<created/>");
    private final AtomicReference<String> requestBody = new AtomicReference<>();
    private final AtomicInteger status = new AtomicInteger(200);

    @BeforeEach
    void startServer() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/exist/rest/db", exchange -> {
            requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] response = responseBody.get().getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(status.get(), response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();
        repository = new ExistKorisnikRepository("http://127.0.0.1:" + server.getAddress().getPort()
                + "/exist/rest", "admin", "test-only", 2, 2);
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    @Test
    void bindsUserInputAsDataAndUsesDatabaseLock() throws Exception {
        String attack = "  O'HARA@example.org  ";
        repository.save(attack, "<user>hash-only</user>");
        var factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        var document = factory.newDocumentBuilder().parse(new java.io.ByteArrayInputStream(
                requestBody.get().getBytes(StandardCharsets.UTF_8)));
        String query = document.getElementsByTagNameNS("http://exist.sourceforge.net/NS/exist", "text").item(0).getTextContent();
        assertFalse(query.contains("o'hara"));
        assertFalse(query.contains("hash-only"));
        assertTrue(query.contains("util:exclusive-lock(doc($lock)"));
        assertTrue(query.contains("collection($collection)/k:korisnik[k:email = $email]"));
        var values = document.getElementsByTagNameNS("http://exist-db.org/xquery/types/serialized", "value");
        assertEquals(2, values.getLength());
        assertTrue(requestBody.get().contains("o'hara@example.org"));
        responseBody.set("<duplicate/>");
        assertThrows(EmailAlreadyExistsException.class, () -> repository.save(attack, "<user/>"));
    }

    @Test
    void findsByUrlIdUsingExternalParameter() {
        String id = "https://example.org/nalozi/O'Hara?a=1&b=2";
        responseBody.set("<missing/>");
        assertTrue(repository.findById(id).isEmpty());
        assertTrue(requestBody.get().contains("@id = $id"));
        assertTrue(requestBody.get().contains("a=1&amp;b=2"));
        responseBody.set("<k:korisnik xmlns:k='https://example.org/autorska-prava/korisnik' id='https://example.org/nalozi/one'/>");
        assertTrue(repository.findById(id).orElseThrow().contains("https://example.org/nalozi/one"));
        status.set(403);
        assertThrows(KorisnikRepositoryException.class, () -> repository.findById(id));
    }

    @Test
    void findsUserAndOnlyExplicitMissingMeansAbsent() throws Exception {
        responseBody.set("<missing/>");
        assertTrue(repository.findByEmail("test@example.org").isEmpty());
        responseBody.set("<k:korisnik xmlns:k='https://example.org/autorska-prava/korisnik'><k:email>test@example.org</k:email></k:korisnik>");
        assertTrue(repository.findByEmail("test@example.org").orElseThrow().contains("test@example.org"));
        for (int code : new int[]{401, 403, 404, 500}) {
            status.set(code);
            assertThrows(KorisnikRepositoryException.class, () -> repository.findByEmail("test@example.org"));
            assertThrows(KorisnikRepositoryException.class, () -> repository.save("test@example.org", "<user/>"));
        }
    }
}
