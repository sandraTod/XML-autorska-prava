package com.example.xml_backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.example.xml_backend.model.a1.ZahtevA1Type;
import com.example.xml_backend.service.impl.A1XmlServiceImpl;

import jakarta.xml.bind.JAXBException;

class A1XmlServiceTest {

    private final A1XmlService service = new A1XmlServiceImpl();
    private String example;

    @BeforeEach
    void loadExample() throws Exception {
        try (InputStream input = getClass().getResourceAsStream("/a1-zahtev-primer.xml")) {
            assertNotNull(input);
            example = new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    @Test
    void readsExampleAndPreservesDataThroughRoundTrip() throws Exception {
        ZahtevA1Type original = service.fromXml(example);
        assertExampleData(original);

        String xml = service.toXml(original);
        ZahtevA1Type restored = service.fromXml(xml);
        assertExampleData(restored);
        // Stabilan ponovni izlaz proverava i ostala polja, nezavisno od prefiksa ulaznog XML-a.
        assertEquals(xml, service.toXml(restored));
    }

    @Test
    void rejectsXmlWithoutTitle() {
        String invalid = example.replace("<a1:naslov>Priče sa zamišljene obale</a1:naslov>", "");
        assertThrows(JAXBException.class, () -> service.fromXml(invalid));
    }

    @Test
    void rejectsObjectWithoutTitle() throws Exception {
        ZahtevA1Type zahtev = service.fromXml(example);
        zahtev.getPodaciGradjanina().getDelo().setNaslov(null);
        assertThrows(JAXBException.class, () -> service.toXml(zahtev));
    }

    @Test
    void rejectsDtdAndExternalEntities() {
        String doctype = "<!DOCTYPE a1:zahtevA1 [<!ENTITY external SYSTEM 'file:///a1-xxe-test.txt'>]>";
        String invalid = example.replace("?>", "?>\n" + doctype)
                .replace("Priče sa zamišljene obale", "&external;");
        assertThrows(JAXBException.class, () -> service.fromXml(invalid));
    }

    private void assertExampleData(ZahtevA1Type zahtev) {
        var podaci = zahtev.getPodaciGradjanina();
        var podnosilac = podaci.getPodnosilac();
        assertNull(podnosilac.getFizickoLice());
        assertNotNull(podnosilac.getPravnoLice());
        assertEquals("Izmišljeni izdavač Dunav DOO", podnosilac.getPravnoLice().getPoslovnoIme());
        assertEquals("Novi Sad", podnosilac.getPravnoLice().getSediste().getMesto());
        assertEquals("prijave@example.org", podnosilac.getKontakt().getEmail());
        var delo = podaci.getDelo();
        assertEquals("Priče sa zamišljene obale", delo.getNaslov());
        var autori = delo.getAutorstvo().getPoznatiAutori().getAutor();
        assertEquals(2, autori.size());
        assertEquals("Mila", autori.get(0).getZiviAutor().getIme());
        assertEquals("Obala", autori.get(0).getPseudonimIliZnak());
        assertNull(autori.get(0).getPreminuliAutor());
        assertEquals("Petar", autori.get(1).getPreminuliAutor().getIme());
        assertEquals(2020, autori.get(1).getPreminuliAutor().getGodinaSmrti().getYear());
        assertNull(autori.get(1).getZiviAutor());
        assertNull(zahtev.getSluzbeniDeo());
    }
}
