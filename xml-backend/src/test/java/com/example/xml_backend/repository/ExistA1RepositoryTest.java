package com.example.xml_backend.repository;

import static org.junit.jupiter.api.Assertions.*;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.io.ByteArrayInputStream;
import javax.xml.parsers.DocumentBuilderFactory;

class ExistA1RepositoryTest {
    private HttpServer server;
    private ExistA1Repository repository;
    private final AtomicInteger status = new AtomicInteger(200);
    private final AtomicReference<String> stored = new AtomicReference<>("<test/>");
    private final AtomicInteger requests = new AtomicInteger();

    @BeforeEach
    void startServer() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/exist/rest/db/autorska-prava/a1/test-1.xml", exchange -> {
            requests.incrementAndGet();
            if ("PUT".equals(exchange.getRequestMethod())) {
                stored.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            }
            byte[] body = stored.get().getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/xml");
            exchange.sendResponseHeaders(status.get(), body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
        repository = new ExistA1Repository("http://127.0.0.1:" + server.getAddress().getPort()
                + "/exist/rest", "admin", "test-only-password", 2, 2);
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    @Test
    void savesAndReadsUtf8Xml() {
        String xml = "<test>Izmišljeni podaci</test>";
        status.set(201);
        repository.save("test-1", xml);
        status.set(200);
        assertEquals(xml, repository.findById("test-1").orElseThrow());
    }

    @Test
    void onlyRead404MeansMissing() {
        status.set(404);
        assertTrue(repository.findById("test-1").isEmpty());
        assertThrows(IllegalStateException.class, () -> repository.save("test-1", "<test/>"));
        for (int error : new int[]{301, 401, 403, 500}) {
            status.set(error);
            assertThrows(IllegalStateException.class, () -> repository.findById("test-1"));
            assertThrows(IllegalStateException.class, () -> repository.save("test-1", "<test/>"));
        }
    }

    @Test
    void rejectsUnsafeIdsBeforeHttpRequest() {
        for (String id : new String[]{"../other", "test.xml", "a/b", "a?x=1", "", "a%2Fb"}) {
            assertThrows(IllegalArgumentException.class, () -> repository.findById(id));
            assertThrows(IllegalArgumentException.class, () -> repository.save(id, "<test/>"));
        }
        assertEquals(0, requests.get());
    }

    @Test
    void listQueryBindsOwnerAndDoesNotHideHttpOrConnectionErrors() throws Exception {
        var received = new AtomicReference<String>();
        String emptyList = "<l:listaZahteva xmlns:l='https://example.org/autorska-prava/a1-lista'/>";
        server.createContext("/exist/rest/db", exchange -> {
            received.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] body = emptyList.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(status.get(), body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        String owner = "https://example.org/nalozi/test-'&-č";
        assertEquals(emptyList, repository.findByOwner(owner));
        var factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        var document = factory.newDocumentBuilder().parse(new ByteArrayInputStream(received.get().getBytes(StandardCharsets.UTF_8)));
        String query = document.getElementsByTagNameNS("http://exist.sourceforge.net/NS/exist", "text").item(0).getTextContent();
        assertFalse(query.contains(owner));
        assertTrue(query.contains("declare variable $vlasnikId as xs:string external"));
        assertTrue(query.contains("a1:zahtevA1[@vlasnikId = $vlasnikId]"));
        assertFalse(query.contains("order by"));
        assertTrue(query.contains("xmldb:get-child-collections('/db')"));
        assertFalse(query.contains("xmldb:collection-available("));
        assertEquals(owner, document.getElementsByTagNameNS("http://exist-db.org/xquery/types/serialized", "value").item(0).getTextContent());
        for (int code : new int[]{401, 403, 404, 500}) {
            status.set(code);
            assertThrows(A1RepositoryException.class, () -> repository.findByOwner(owner));
        }
        server.stop(0);
        assertThrows(A1RepositoryException.class, () -> repository.findByOwner(owner));
    }
}
