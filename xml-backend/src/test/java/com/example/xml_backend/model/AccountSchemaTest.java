package com.example.xml_backend.model;

import static org.junit.jupiter.api.Assertions.*;

import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import org.junit.jupiter.api.Test;
import org.xml.sax.SAXException;
import jakarta.xml.bind.JAXBContext;
import com.example.xml_backend.model.registracija.RegistracijaType;
import com.example.xml_backend.model.korisnik.UlogaType;

class AccountSchemaTest {
    private String example(String name) throws Exception {
        try (var input = getClass().getResourceAsStream("/" + name + "-primer.xml")) {
            assertNotNull(input);
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private Schema schema(String name) throws Exception {
        var factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        factory.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        factory.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        return factory.newSchema(getClass().getResource("/" + name + ".xsd"));
    }

    private void validate(Schema schema, String xml) throws Exception {
        schema.newValidator().validate(new StreamSource(new StringReader(xml)));
    }

    @Test
    void examplesAreValidAndEveryRequiredFieldIsRequired() throws Exception {
        for (String name : new String[]{"registracija", "korisnik"}) {
            Schema schema = schema(name);
            String xml = example(name);
            validate(schema, xml);
            String prefix = name.equals("registracija") ? "r" : "k";
            String[] fields = name.equals("registracija")
                    ? new String[]{"ime", "prezime", "email", "lozinka"}
                    : new String[]{"ime", "prezime", "email", "lozinkaHash", "uloga"};
            for (String field : fields) {
                String invalid = xml.replaceAll("<" + prefix + ":" + field + ">[^<]*</" + prefix + ":" + field + ">", "");
                assertNotEquals(xml, invalid);
                assertThrows(SAXException.class, () -> validate(schema, invalid), field);
            }
            if (name.equals("korisnik")) {
                String noId = xml.replaceAll(" id=\"[^\"]*\"", "");
                assertThrows(SAXException.class, () -> validate(schema, noId));
                String relativeId = xml.replaceAll(" id=\"[^\"]*\"", " id=\"relative-id\"");
                assertThrows(SAXException.class, () -> validate(schema, relativeId));
                assertThrows(SAXException.class, () -> validate(schema, xml.replace("GRADJANIN", "ADMIN")));
                validate(schema, xml.replace("GRADJANIN", "SLUZBENIK"));
                assertEquals(UlogaType.GRADJANIN, UlogaType.fromValue("GRADJANIN"));
                assertFalse(xml.contains("RazvojTest123!"));
            }
        }
    }

    @Test
    void registrationRejectsIdAndRoleAndPreservesPasswordWhitespace() throws Exception {
        String xml = example("registracija");
        Schema schema = schema("registracija");
        String withId = xml.replace("<r:registracija ", "<r:registracija id=\"https://example.org/nalog/1\" ");
        assertThrows(SAXException.class, () -> validate(schema, withId));
        String withRole = xml.replace("</r:registracija>", "<r:uloga>SLUZBENIK</r:uloga></r:registracija>");
        assertThrows(SAXException.class, () -> validate(schema, withRole));
        String withIdElement = xml.replace("</r:registracija>", "<r:id>https://example.org/nalog/1</r:id></r:registracija>");
        assertThrows(SAXException.class, () -> validate(schema, withIdElement));
        String password = "  razvoj  test  ";
        String spaced = xml.replace("RazvojTest123!", password);
        var unmarshaller = JAXBContext.newInstance(RegistracijaType.class).createUnmarshaller();
        unmarshaller.setSchema(schema);
        var registration = unmarshaller.unmarshal(new StreamSource(new StringReader(spaced)), RegistracijaType.class).getValue();
        assertEquals(password, registration.getLozinka());
    }
}
