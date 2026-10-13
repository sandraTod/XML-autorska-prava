package com.example.xml_backend.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import com.example.xml_backend.service.impl.A1XmlServiceImpl;

@EnabledIfEnvironmentVariable(named = "EXIST_DB_INTEGRATION_TEST", matches = "true")
class ExistA1RepositoryIntegrationTest {
    @Test
    void savesAndReadsExampleInRealDatabase() throws Exception {
        var repository = new ExistA1Repository(
                System.getenv().getOrDefault("EXIST_DB_URL", "http://localhost:8081/exist/rest"),
                System.getenv().getOrDefault("EXIST_DB_USERNAME", "admin"),
                System.getenv().getOrDefault("EXIST_DB_PASSWORD", ""), 5, 15);
        var xmlService = new A1XmlServiceImpl();
        String example;
        try (var input = getClass().getResourceAsStream("/a1-zahtev-primer.xml")) {
            example = new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
        var original = xmlService.fromXml(example);
        String id = "integration-" + UUID.randomUUID();
        repository.save(id, xmlService.toXml(original));
        var restored = xmlService.fromXml(repository.findById(id).orElseThrow());
        assertEquals(xmlService.toXml(original), xmlService.toXml(restored));
        // Dokument ostaje u bazi radi ručne provere; njegova oznaka nije poverljiv podatak.
        System.out.println("Sačuvan i pročitan A-1 dokument: " + id);
    }
}
