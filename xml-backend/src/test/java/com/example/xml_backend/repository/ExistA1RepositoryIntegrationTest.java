package com.example.xml_backend.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.ArrayList;
import java.util.Locale;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import com.example.xml_backend.service.impl.A1XmlServiceImpl;
import com.example.xml_backend.service.impl.A1PretragaXmlServiceImpl;
import com.example.xml_backend.service.impl.ResenjeXmlServiceImpl;

@EnabledIfEnvironmentVariable(named = "EXIST_DB_INTEGRATION_TEST", matches = "true")
class ExistA1RepositoryIntegrationTest {
    @Test
    void searchesApprovedRequestsAcrossOwnersWithLiteralCaseAndWhitespaceNormalization() throws Exception {
        String url = System.getenv().getOrDefault("EXIST_DB_URL", "http://localhost:8081/exist/rest");
        String username = System.getenv().getOrDefault("EXIST_DB_USERNAME", "admin");
        String password = System.getenv().getOrDefault("EXIST_DB_PASSWORD", "");
        var requests = new ExistA1Repository(url, username, password, 5, 15);
        var resolutions = new ExistResenjeRepository(url, username, password, 5, 15);
        var requestXml = new A1XmlServiceImpl();
        var resolutionXml = new ResenjeXmlServiceImpl();
        var searchXml = new A1PretragaXmlServiceImpl();
        String marker = "search-" + UUID.randomUUID();
        var urls = new ArrayList<String>();
        for (int index = 0; index < 4; index++) {
            String id = marker + "-" + index;
            String requestUrl = "http://localhost:8080/api/a1/zahtevi/" + id;
            urls.add(requestUrl);
            var request = requestXml.fromXml(resource("a1-zahtev-primer.xml"));
            request.setId(requestUrl);
            request.setVlasnikId("https://example.org/nalozi/owner-" + index);
            request.getPodaciGradjanina().getDelo().setNaslov(marker + "  PriČE\tObale [.*]");
            requests.save(id, requestXml.toXml(request));
            if (index < 3) {
                var resolution = resolutionXml.fromXml(resource(index == 2
                        ? "resenje-odbijanje-primer.xml" : "resenje-odobrenje-primer.xml"));
                resolution.setId(requestUrl + "/resenje");
                resolution.setZahtevRef(requestUrl);
                resolutions.saveIfAbsent(id, resolutionXml.toXml(resolution));
            }
        }
        var results = searchXml.fromXml(requests.searchAccepted(" \n" + marker.toUpperCase(Locale.ROOT) + "  PRIČE OBALE [.*]\t"));
        assertEquals(2, results.getDelo().size());
        var ids = results.getDelo().stream().map(item -> item.getZahtevId()).toList();
        assertTrue(ids.containsAll(urls.subList(0, 2)));
        assertEquals(0, searchXml.fromXml(requests.searchAccepted(marker + " absent")).getDelo().size());
        var allIds = searchXml.fromXml(requests.searchAccepted(" \t\n"))
                .getDelo().stream().map(item -> item.getZahtevId()).toList();
        assertTrue(allIds.containsAll(urls.subList(0, 2)));
        assertFalse(allIds.contains(urls.get(2)));
        assertFalse(allIds.contains(urls.get(3)));
        // Uniquely named development documents remain in the database for inspection.
    }

    private String resource(String name) throws Exception {
        try (var input = getClass().getResourceAsStream("/" + name)) {
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

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
