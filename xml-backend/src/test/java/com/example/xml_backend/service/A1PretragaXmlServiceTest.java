package com.example.xml_backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import com.example.xml_backend.model.a1pretraga.RezultatiPretrageType;
import com.example.xml_backend.service.impl.A1PretragaXmlServiceImpl;
import jakarta.xml.bind.JAXBException;

class A1PretragaXmlServiceTest {
    private final A1PretragaXmlService xml = new A1PretragaXmlServiceImpl();

    @Test
    void validatesPopulatedAndEmptyResultsInBothDirections() throws Exception {
        String example;
        try (var input = getClass().getResourceAsStream("/a1-pretraga-primer.xml")) {
            example = new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
        var result = xml.fromXml(example);
        assertEquals(2, result.getDelo().size());
        assertEquals(result.getDelo().get(0).getZahtevId(), xml.fromXml(xml.toXml(result)).getDelo().get(0).getZahtevId());
        assertEquals(0, xml.fromXml(xml.toXml(new RezultatiPretrageType())).getDelo().size());
        result.getDelo().get(0).setZahtevId(null);
        assertThrows(JAXBException.class, () -> xml.toXml(result));
        String privateData = example.replace("</p:delo>", "<p:podnosilac>Tajni podaci</p:podnosilac></p:delo>");
        assertThrows(JAXBException.class, () -> xml.fromXml(privateData));
        assertThrows(JAXBException.class, () -> xml.fromXml("<!DOCTYPE rezultatiPretrage [<!ENTITY x SYSTEM 'file:///secret'>]><rezultatiPretrage xmlns='https://example.org/autorska-prava/a1-pretraga'/>"));
    }
}
