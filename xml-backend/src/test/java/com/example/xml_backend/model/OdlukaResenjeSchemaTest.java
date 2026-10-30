package com.example.xml_backend.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import org.junit.jupiter.api.Test;
import org.xml.sax.SAXException;
import com.example.xml_backend.model.odluka.OdlukaType;
import com.example.xml_backend.model.resenje.ResenjeType;
import jakarta.xml.bind.JAXBContext;

class OdlukaResenjeSchemaTest {
    private String example(String model, String outcome) throws Exception {
        try (var input = getClass().getResourceAsStream("/" + model + "-" + outcome + "-primer.xml")) {
            assertNotNull(input);
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private Schema schema(String model) throws Exception {
        var factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        factory.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        factory.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        return factory.newSchema(getClass().getResource("/" + model + ".xsd"));
    }

    private void validate(Schema schema, String xml) throws Exception {
        schema.newValidator().validate(new StreamSource(new StringReader(xml)));
    }

    private String removeElement(String xml, String prefix, String field) {
        String result = xml.replaceAll("(?s)<" + prefix + ":" + field + "(?:\\s[^>]*)?>.*?</" + prefix + ":" + field + ">", "");
        assertNotEquals(xml, result);
        return result;
    }

    @Test
    void allExamplesValidateAndBothModelsRoundTripThroughGeneratedClasses() throws Exception {
        Schema decisionSchema = schema("odluka");
        Schema resolutionSchema = schema("resenje");
        var decisionContext = JAXBContext.newInstance(OdlukaType.class);
        var resolutionContext = JAXBContext.newInstance(ResenjeType.class);
        for (String outcome : new String[]{"odobrenje", "odbijanje"}) {
            String decisionXml = example("odluka", outcome);
            String resolutionXml = example("resenje", outcome);
            validate(decisionSchema, decisionXml);
            validate(resolutionSchema, resolutionXml);
            var reader = decisionContext.createUnmarshaller();
            reader.setSchema(decisionSchema);
            var decision = reader.unmarshal(new StreamSource(new StringReader(decisionXml)), OdlukaType.class).getValue();
            assertEquals(outcome.equals("odobrenje"), decision.getOdobrenje() != null);
            var writer = decisionContext.createMarshaller();
            writer.setSchema(decisionSchema);
            StringWriter output = new StringWriter();
            writer.marshal(new com.example.xml_backend.model.odluka.ObjectFactory().createOdluka(decision), output);
            validate(decisionSchema, output.toString());

            var resolutionReader = resolutionContext.createUnmarshaller();
            resolutionReader.setSchema(resolutionSchema);
            var resolution = resolutionReader.unmarshal(new StreamSource(new StringReader(resolutionXml)), ResenjeType.class).getValue();
            assertEquals(outcome.equals("odobrenje"), resolution.getOdobrenje() != null);
            assertNotNull(resolution.getSluzbenik().getId());
            assertEquals("Jelena", resolution.getSluzbenik().getIme());
            assertEquals("2026-10-28", resolution.getDatumOdluke().toXMLFormat());
            assertEquals(resolution.getZahtevRef() + "/resenje", resolution.getId());
            if (outcome.equals("odbijanje")) {
                assertEquals(decision.getOdbijanje().getObrazlozenje(), resolution.getOdbijanje().getObrazlozenje());
            }
            var resolutionWriter = resolutionContext.createMarshaller();
            resolutionWriter.setSchema(resolutionSchema);
            StringWriter resolutionOutput = new StringWriter();
            resolutionWriter.marshal(new com.example.xml_backend.model.resenje.ObjectFactory().createResenje(resolution), resolutionOutput);
            var restored = resolutionReader.unmarshal(new StreamSource(new StringReader(resolutionOutput.toString())), ResenjeType.class).getValue();
            assertEquals(resolution.getId(), restored.getId());
            assertEquals(resolution.getZahtevRef(), restored.getZahtevRef());
        }
    }

    @Test
    void rejectionRequiresNonblankReasonAndChoicesAreExclusive() throws Exception {
        for (String model : new String[]{"odluka", "resenje"}) {
            Schema schema = schema(model);
            String prefix = model.equals("odluka") ? "o" : "r";
            String rejection = example(model, "odbijanje");
            assertThrows(SAXException.class, () -> validate(schema, removeElement(rejection, prefix, "obrazlozenje")));
            for (String blank : new String[]{"", "   "}) {
                String invalid = rejection.replaceAll("<" + prefix + ":obrazlozenje>[^<]*</" + prefix + ":obrazlozenje>",
                        "<" + prefix + ":obrazlozenje>" + blank + "</" + prefix + ":obrazlozenje>");
                assertThrows(SAXException.class, () -> validate(schema, invalid));
            }
            String both = rejection.replace("</" + prefix + ":" + model + ">",
                    "<" + prefix + ":odobrenje/></" + prefix + ":" + model + ">");
            assertThrows(SAXException.class, () -> validate(schema, both));
            assertThrows(SAXException.class, () -> validate(schema, removeElement(rejection, prefix, "odbijanje")));
        }
        String approval = example("resenje", "odobrenje");
        Schema schema = schema("resenje");
        assertThrows(SAXException.class, () -> validate(schema, removeElement(approval, "r", "sifraDela")));
        assertThrows(SAXException.class, () -> validate(schema, approval.replaceAll("<r:sifraDela>[^<]*</r:sifraDela>", "<r:sifraDela>   </r:sifraDela>")));
    }

    @Test
    void resolutionRequiresCommonFieldsAndUrlIdentifiers() throws Exception {
        Schema schema = schema("resenje");
        String xml = example("resenje", "odobrenje");
        for (String field : new String[]{"zahtevRef", "datumOdluke", "sluzbenik", "ime", "prezime"}) {
            assertThrows(SAXException.class, () -> validate(schema, removeElement(xml, "r", field)), field);
        }
        assertThrows(SAXException.class, () -> validate(schema, xml.replaceFirst("id=\"[^\"]*\"", "")));
        assertThrows(SAXException.class, () -> validate(schema, xml.replaceFirst("id=\"[^\"]*\"", "id=\"relative-id\"")));
        assertThrows(SAXException.class, () -> validate(schema, xml.replaceAll("<r:zahtevRef>[^<]*</r:zahtevRef>", "<r:zahtevRef>relative-id</r:zahtevRef>")));
        assertThrows(SAXException.class, () -> validate(schema, xml.replaceFirst("<r:sluzbenik id=\"[^\"]*\"", "<r:sluzbenik")));
        assertThrows(SAXException.class, () -> validate(schema, xml.replace("2026-10-28", "not-a-date")));
    }

    @Test
    void decisionDoesNotAcceptBackendFields() throws Exception {
        Schema schema = schema("odluka");
        String approval = example("odluka", "odobrenje");
        assertThrows(SAXException.class, () -> validate(schema, approval.replace("<o:odluka ", "<o:odluka id=\"https://example.org/resenja/1\" ")));
        for (String field : new String[]{"datumOdluke", "sluzbenik", "zahtevRef", "sifraDela"}) {
            String invalid = approval.replace("</o:odluka>", "<o:" + field + ">test</o:" + field + "></o:odluka>");
            assertThrows(SAXException.class, () -> validate(schema, invalid));
        }
    }
}
