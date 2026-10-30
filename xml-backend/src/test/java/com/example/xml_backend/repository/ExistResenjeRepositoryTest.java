package com.example.xml_backend.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import javax.xml.parsers.DocumentBuilderFactory;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ExistResenjeRepositoryTest {
    private HttpServer server;
    private ExistResenjeRepository repository;
    private final AtomicReference<String> response = new AtomicReference<>("<created/>");
    private final AtomicReference<String> body = new AtomicReference<>();
    private final AtomicReference<String> method = new AtomicReference<>();
    private final AtomicReference<String> path = new AtomicReference<>();
    private final AtomicReference<String> authorization = new AtomicReference<>();
    private final AtomicInteger status = new AtomicInteger(200);

    @BeforeEach
    void start() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/exist/rest/db", exchange -> {
            body.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            method.set(exchange.getRequestMethod());
            path.set(exchange.getRequestURI().getPath());
            authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
            byte[] bytes = response.get().getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(status.get(), bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        server.start();
        repository = new ExistResenjeRepository("http://127.0.0.1:" + server.getAddress().getPort()
                + "/exist/rest", "admin", "test-only", 2, 2);
    }

    @AfterEach
    void stop() {
        server.stop(0);
    }

    @Test
    void bindsXmlAsDataAndChecksDuplicateInsideRequestDocumentLock() throws Exception {
        String xml = "<resenje><obrazlozenje>A &amp; B</obrazlozenje></resenje>";
        repository.saveIfAbsent("test-one", xml);
        assertEquals("POST", method.get());
        assertEquals("/exist/rest/db", path.get());
        assertTrue(authorization.get().startsWith("Basic "));
        var factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        var document = factory.newDocumentBuilder().parse(new ByteArrayInputStream(body.get().getBytes(StandardCharsets.UTF_8)));
        String query = document.getElementsByTagNameNS("http://exist.sourceforge.net/NS/exist", "text").item(0).getTextContent();
        assertFalse(query.contains(xml));
        assertFalse(query.contains("test-one"));
        int lock = query.indexOf("util:exclusive-lock(doc(concat($requests, '/', $filename))");
        assertTrue(lock > 0);
        assertTrue(query.indexOf("xmldb:get-child-resources($collection) = $filename") > lock);
        assertTrue(query.indexOf("xmldb:store(") > lock);
        assertFalse(query.contains("update "));
        var values = document.getElementsByTagNameNS("http://exist-db.org/xquery/types/serialized", "value");
        assertEquals(2, values.getLength());
        assertTrue(xml.equals(values.item(0).getTextContent()) || xml.equals(values.item(1).getTextContent()));
        response.set("<?xml version=\"1.0\"?>\n<duplicate />");
        assertThrows(ResenjeAlreadyExistsException.class, () -> repository.saveIfAbsent("test-one", xml));
        response.set("<missing/>");
        assertThrows(ResenjeNotFoundException.class, () -> repository.saveIfAbsent("test-one", xml));
        response.set("<unexpected/>");
        assertThrows(A1RepositoryException.class, () -> repository.saveIfAbsent("test-one", xml));
    }

    @Test
    void onlyRead404MeansMissingAndUnsafeIdIsRejected() {
        response.set("<resenje/>");
        assertEquals("<resenje/>", repository.findById("test-one").orElseThrow());
        assertEquals("GET", method.get());
        assertEquals("/exist/rest/db/autorska-prava/resenja/test-one.xml", path.get());
        status.set(404);
        assertTrue(repository.findById("test-one").isEmpty());
        for (int code : new int[]{401, 403, 404, 500}) {
            status.set(code);
            assertThrows(A1RepositoryException.class, () -> repository.saveIfAbsent("test-one", "<resenje/>"));
            if (code != 404) assertThrows(A1RepositoryException.class, () -> repository.findById("test-one"));
        }
        assertThrows(IllegalArgumentException.class, () -> repository.saveIfAbsent("../other", "<resenje/>"));
        assertThrows(IllegalArgumentException.class, () -> repository.findById("test.xml"));
        server.stop(0);
        assertThrows(A1RepositoryException.class, () -> repository.findById("test-one"));
    }
}
