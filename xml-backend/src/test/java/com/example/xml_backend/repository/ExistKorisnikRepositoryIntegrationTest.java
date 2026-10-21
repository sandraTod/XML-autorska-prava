package com.example.xml_backend.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import com.example.xml_backend.service.impl.KorisnikXmlServiceImpl;

@EnabledIfEnvironmentVariable(named = "KORISNIK_DB_INTEGRATION_TEST", matches = "true")
class ExistKorisnikRepositoryIntegrationTest {
    private ExistKorisnikRepository repository() {
        return new ExistKorisnikRepository(
                System.getenv().getOrDefault("EXIST_DB_URL", "http://localhost:8081/exist/rest"),
                System.getenv().getOrDefault("EXIST_DB_USERNAME", "admin"),
                System.getenv().getOrDefault("EXIST_DB_PASSWORD", ""), 5, 15);
    }

    @Test
    void concurrentRegistrationsAcrossClientsCreateExactlyOneUser() throws Exception {
        var firstRepository = repository();
        var secondRepository = repository();
        var xmlService = new KorisnikXmlServiceImpl();
        String example;
        try (var input = getClass().getResourceAsStream("/korisnik-primer.xml")) {
            example = new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
        var user = xmlService.fromXml(example);
        user.setId("https://example.org/nalozi/" + UUID.randomUUID());
        String email = "concurrent-" + UUID.randomUUID() + "@example.org";
        user.setEmail(email);
        String xml = xmlService.toXml(user);
        var start = new CountDownLatch(1);
        var executor = Executors.newFixedThreadPool(2);
        try {
            Callable<Boolean> first = () -> save(firstRepository, email, xml, start);
            Callable<Boolean> second = () -> save(secondRepository, email, xml, start);
            var firstResult = executor.submit(first);
            var secondResult = executor.submit(second);
            start.countDown();
            List<Boolean> results = List.of(firstResult.get(), secondResult.get());
            assertEquals(1, results.stream().filter(Boolean::booleanValue).count());
            assertTrue(firstRepository.findByEmail(email.toUpperCase(java.util.Locale.ROOT)).isPresent());
            assertEquals(email, xmlService.fromXml(firstRepository.findByEmail(email).orElseThrow()).getEmail());
        } finally {
            executor.shutdownNow();
        }
        // Razvojni nalog ostaje u bazi. Ne ispisuju se XML ni hash.
    }

    private boolean save(KorisnikRepository repository, String email, String xml, CountDownLatch start) throws Exception {
        start.await();
        try {
            repository.save(email, xml);
            return true;
        } catch (EmailAlreadyExistsException duplicate) {
            return false;
        }
    }
}
