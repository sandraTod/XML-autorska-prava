package com.example.xml_backend.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import com.example.xml_backend.service.impl.ResenjeXmlServiceImpl;

@EnabledIfEnvironmentVariable(named = "RESENJE_DB_INTEGRATION_TEST", matches = "true")
class ExistResenjeRepositoryIntegrationTest {
    @Test
    void concurrentDecisionsCreateExactlyOneResolutionAndLeaveRequestUnchanged() throws Exception {
        String url = System.getenv().getOrDefault("EXIST_DB_URL", "http://localhost:8081/exist/rest");
        String username = System.getenv().getOrDefault("EXIST_DB_USERNAME", "admin");
        String password = System.getenv().getOrDefault("EXIST_DB_PASSWORD", "");
        var requests = new ExistA1Repository(url, username, password, 5, 15);
        var first = new ExistResenjeRepository(url, username, password, 5, 15);
        var second = new ExistResenjeRepository(url, username, password, 5, 15);
        String id = "decision-test-" + UUID.randomUUID();
        requests.save(id, resource("a1-zahtev-primer.xml"));
        String unchanged = requests.findById(id).orElseThrow();
        var codec = new ResenjeXmlServiceImpl();
        String requestUrl = "http://localhost:8080/api/a1/zahtevi/" + id;
        var approval = codec.fromXml(resource("resenje-odobrenje-primer.xml"));
        approval.setId(requestUrl + "/resenje");
        approval.setZahtevRef(requestUrl);
        var rejection = codec.fromXml(resource("resenje-odbijanje-primer.xml"));
        rejection.setId(requestUrl + "/resenje");
        rejection.setZahtevRef(requestUrl);
        String approvedXml = codec.toXml(approval);
        String rejectedXml = codec.toXml(rejection);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        var executor = Executors.newFixedThreadPool(2);
        try {
            var one = executor.submit(() -> attempt(first, id, approvedXml, ready, start));
            var two = executor.submit(() -> attempt(second, id, rejectedXml, ready, start));
            assertTrue(ready.await(5, TimeUnit.SECONDS));
            start.countDown();
            assertEquals(1, one.get(30, TimeUnit.SECONDS) + two.get(30, TimeUnit.SECONDS));
            String stored = codec.toXml(codec.fromXml(first.findById(id).orElseThrow()));
            assertTrue(stored.equals(approvedXml) || stored.equals(rejectedXml));
            assertEquals(unchanged, requests.findById(id).orElseThrow());
        } finally {
            start.countDown();
            executor.shutdownNow();
        }
        // The uniquely named development documents remain available for manual inspection.
    }

    private int attempt(ExistResenjeRepository repository, String id, String xml,
            CountDownLatch ready, CountDownLatch start) throws Exception {
        ready.countDown();
        if (!start.await(5, TimeUnit.SECONDS)) throw new IllegalStateException("Test synchronization failed");
        try {
            repository.saveIfAbsent(id, xml);
            return 1;
        } catch (ResenjeAlreadyExistsException exception) {
            return 0;
        }
    }

    private String resource(String name) throws Exception {
        try (var input = getClass().getResourceAsStream("/" + name)) {
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
