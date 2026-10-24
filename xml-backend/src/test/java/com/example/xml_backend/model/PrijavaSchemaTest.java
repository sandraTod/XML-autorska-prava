package com.example.xml_backend.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import org.junit.jupiter.api.Test;
import org.xml.sax.SAXException;
import jakarta.xml.bind.JAXBContext;
import com.example.xml_backend.model.prijava.PrijavaType;
import com.example.xml_backend.model.prijavaodgovor.PrijavaOdgovorType;

class PrijavaSchemaTest {
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
    void examplesValidateAndMissingFieldsAreRejected() throws Exception {
        for (String name : new String[]{"prijava", "prijava-odgovor"}) {
            String xml = example(name);
            Schema schema = schema(name);
            validate(schema, xml);
            String[] fields = name.equals("prijava")
                    ? new String[]{"email", "lozinka"} : new String[]{"token", "expiresAt"};
            for (String field : fields) {
                String invalid = xml.replaceAll("<p:" + field + ">[^<]*</p:" + field + ">", "");
                assertNotEquals(xml, invalid);
                assertThrows(SAXException.class, () -> validate(schema, invalid), field);
            }
            String requiredText = fields[0];
            String blank = xml.replaceAll("<p:" + requiredText + ">[^<]*</p:" + requiredText + ">",
                    "<p:" + requiredText + ">   </p:" + requiredText + ">");
            assertThrows(SAXException.class, () -> validate(schema, blank));
        }
    }

    @Test
    void generatedLoginModelPreservesPasswordWhitespace() throws Exception {
        String password = "  razvoj  lozinka  ";
        String xml = example("prijava").replace("RazvojTest123!", password);
        var unmarshaller = JAXBContext.newInstance(PrijavaType.class).createUnmarshaller();
        unmarshaller.setSchema(schema("prijava"));
        PrijavaType login = unmarshaller.unmarshal(new StreamSource(new StringReader(xml)), PrijavaType.class).getValue();
        assertEquals(password, login.getLozinka());
        assertEquals("mila.primer@example.org", login.getEmail());
    }

    @Test
    void responseHasDateTimeAndRejectsSecretsAndRefreshToken() throws Exception {
        String xml = example("prijava-odgovor");
        Schema schema = schema("prijava-odgovor");
        assertThrows(SAXException.class, () -> validate(schema, xml.replace("2026-10-24T10:00:00Z", "not-a-date")));
        for (String forbidden : new String[]{"lozinka", "lozinkaHash", "refreshToken"}) {
            String invalid = xml.replace("</p:prijavaOdgovor>", "<p:" + forbidden + ">test</p:" + forbidden + "></p:prijavaOdgovor>");
            assertThrows(SAXException.class, () -> validate(schema, invalid));
        }
        var unmarshaller = JAXBContext.newInstance(PrijavaOdgovorType.class).createUnmarshaller();
        unmarshaller.setSchema(schema);
        var response = unmarshaller.unmarshal(new StreamSource(new StringReader(xml)), PrijavaOdgovorType.class).getValue();
        assertEquals("NEFUNKCIONALNI-RAZVOJNI-TOKEN-NIJE-JWT", response.getToken());
        assertEquals("2026-10-24T10:00:00Z", response.getExpiresAt().toXMLFormat());
    }
}
