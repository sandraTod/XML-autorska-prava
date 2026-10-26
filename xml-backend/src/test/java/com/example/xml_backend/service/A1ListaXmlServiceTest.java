package com.example.xml_backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import com.example.xml_backend.model.a1.ListaZahtevaType;
import com.example.xml_backend.service.impl.A1ListaXmlServiceImpl;
import com.example.xml_backend.service.impl.A1XmlServiceImpl;
import jakarta.xml.bind.JAXBException;
import org.junit.jupiter.api.Test;

class A1ListaXmlServiceTest {
    private final A1ListaXmlService service = new A1ListaXmlServiceImpl();

    @Test
    void emptyAndMultipleDocumentsValidateWithSharedA1Model() throws Exception {
        var list = new ListaZahtevaType();
        assertTrue(service.fromXml(service.toXml(list)).getZahtevA1().isEmpty());
        String example;
        try (var input = getClass().getResourceAsStream("/a1-zahtev-primer.xml")) {
            example = new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
        for (int index = 0; index < 2; index++) {
            list.getZahtevA1().add(new A1XmlServiceImpl().fromXml(example));
        }
        assertEquals(2, service.fromXml(service.toXml(list)).getZahtevA1().size());
        list.getZahtevA1().get(0).getPodaciGradjanina().getDelo().setNaslov(null);
        assertThrows(JAXBException.class, () -> service.toXml(list));
    }

    @Test
    void externalSchemaHintsAreIgnoredAndDtdIsRejected() throws Exception {
        String xml = "<l:listaZahteva xmlns:l='https://example.org/autorska-prava/a1-lista' "
                + "xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance' "
                + "xsi:schemaLocation='https://example.org/autorska-prava/a1-lista http://127.0.0.1:1/forbidden.xsd'/>";
        assertTrue(service.fromXml(xml).getZahtevA1().isEmpty());
        assertThrows(JAXBException.class, () -> service.fromXml("<!DOCTYPE listaZahteva SYSTEM 'file:///forbidden'>" + xml));
        assertThrows(JAXBException.class, () -> service.fromXml(
                "<l:listaZahteva xmlns:l='https://example.org/autorska-prava/a1-lista' "
                        + "xmlns:a1='https://example.org/autorska-prava/a1'><a1:zahtevA1/></l:listaZahteva>"));
    }
}
