package com.example.xml_backend.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.xpath;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Optional;
import java.util.List;
import java.util.Properties;
import org.mockito.ArgumentCaptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import com.example.xml_backend.model.korisnik.KorisnikType;
import com.example.xml_backend.model.korisnik.UlogaType;
import com.example.xml_backend.repository.A1Repository;
import com.example.xml_backend.repository.A1RepositoryException;
import com.example.xml_backend.repository.KorisnikRepository;
import com.example.xml_backend.repository.ResenjeRepository;
import com.example.xml_backend.repository.ResenjeAlreadyExistsException;
import com.example.xml_backend.service.A1XmlService;
import com.example.xml_backend.service.JwtTokenService;
import com.example.xml_backend.service.KorisnikXmlService;
import com.example.xml_backend.service.ResenjeService;
import com.example.xml_backend.service.ResenjeXmlService;
import com.example.xml_backend.service.ResenjePdfService;
import com.example.xml_backend.service.impl.ResenjeXhtmlServiceImpl;
import jakarta.mail.Multipart;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;

@SpringBootTest(properties = "app.mail.from=no-reply@example.org")
class ResenjeControllerTest {
    @Autowired private WebApplicationContext context;
    @Autowired private A1XmlService requestXml;
    @Autowired private KorisnikXmlService userXml;
    @Autowired private ResenjeXmlService resolutionXml;
    @Autowired private JwtTokenService tokens;
    @Autowired private ResenjeService service;
    @MockitoBean private A1Repository requests;
    @MockitoBean private KorisnikRepository users;
    @MockitoBean private ResenjeRepository resolutions;
    @MockitoBean private JavaMailSender mailSender;
    @MockitoSpyBean private ResenjePdfService pdfService;
    private MockMvc mvc;
    private KorisnikType official;
    private String officialToken;
    private String citizenToken;
    private static final String OWNER = "https://example.org/nalozi/gradjanin";
    private static final String POST = "/api/sluzbenik/a1/zahtevi/";
    private static final String GET = "/api/a1/zahtevi/";

    @BeforeEach
    void setup() throws Exception {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        official = userXml.fromXml(resource("sluzbenik-primer.xml"));
        officialToken = tokens.issue(official).token();
        KorisnikType citizen = userXml.fromXml(resource("korisnik-primer.xml"));
        citizen.setId(OWNER);
        citizen.setUloga(UlogaType.GRADJANIN);
        citizenToken = tokens.issue(citizen).token();
        when(users.findById(OWNER)).thenReturn(Optional.of(userXml.toXml(citizen)));
        when(mailSender.createMimeMessage()).thenAnswer(invocation -> new MimeMessage(Session.getInstance(new Properties())));
        when(users.findById(official.getId())).thenReturn(Optional.of(userXml.toXml(official)));
        request("one", OWNER);
        request("two", OWNER);
    }

    private void request(String id, String owner) throws Exception {
        var request = requestXml.fromXml(resource("a1-zahtev-primer.xml"));
        request.setId("http://localhost:8080/api/a1/zahtevi/" + id);
        request.setVlasnikId(owner);
        when(requests.findById(id)).thenReturn(Optional.of(requestXml.toXml(request)));
    }

    @Test
    void sendsBothDecisionsAfterSaveToAccountEmailWithPdfAttachment() throws Exception {
        byte[] pdf = "%PDF-development-attachment".getBytes(StandardCharsets.UTF_8);
        doReturn(pdf).when(pdfService).toPdf(anyString());
        String[] ids = {"one", "two"};
        String[] outcomes = {"odobrenje", "odbijanje"};
        for (int i = 0; i < ids.length; i++) {
            String saved = mvc.perform(post(POST + ids[i] + "/resenje").header("Authorization", "Bearer " + officialToken)
                    .contentType(MediaType.APPLICATION_XML).content(resource("odluka-" + outcomes[i] + "-primer.xml")))
                    .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
            verify(pdfService).toPdf(saved);
            when(resolutions.findById(ids[i])).thenReturn(Optional.of(saved));
        }
        var sent = ArgumentCaptor.forClass(MimeMessage.class);
        var order = inOrder(resolutions, mailSender);
        for (String id : ids) {
            order.verify(resolutions).saveIfAbsent(eq(id), anyString());
            order.verify(mailSender).send(sent.capture());
        }
        for (int i = 0; i < ids.length; i++) {
            MimeMessage message = sent.getAllValues().get(i);
            message.saveChanges();
            assertEquals("mila.primer@example.org", message.getAllRecipients()[0].toString());
            assertEquals("no-reply@example.org", message.getFrom()[0].toString());
            assertEquals("Doneto je rešenje o A-1 zahtevu", message.getSubject());
            Multipart body = (Multipart) message.getContent();
            Multipart text = (Multipart) body.getBodyPart(0).getContent();
            assertTrue(text.getBodyPart(0).getContent().toString().contains("u prilogu se nalazi rešenje"));
            var attachment = body.getBodyPart(1);
            assertEquals("resenje-" + ids[i] + ".pdf", attachment.getFileName());
            assertTrue(attachment.isMimeType("application/pdf"));
            assertArrayEquals(pdf, attachment.getInputStream().readAllBytes());
        }
        mvc.perform(get(GET + "one/resenje/pdf").header("Authorization", "Bearer " + citizenToken))
                .andExpect(status().isOk());
        doThrow(new ResenjeAlreadyExistsException()).when(resolutions).saveIfAbsent(eq("one"), anyString());
        mvc.perform(post(POST + "one/resenje").header("Authorization", "Bearer " + officialToken)
                .contentType(MediaType.APPLICATION_XML).content(resource("odluka-odobrenje-primer.xml")))
                .andExpect(status().isConflict());
        verify(mailSender, org.mockito.Mockito.times(2)).send(org.mockito.ArgumentMatchers.any(MimeMessage.class));
    }

    @Test
    void mailAndPdfFailuresDoNotUndoSavedResolution() throws Exception {
        doThrow(new MailSendException("TEST SMTP failure")).when(mailSender)
                .send(org.mockito.ArgumentMatchers.any(MimeMessage.class));
        mvc.perform(post(POST + "one/resenje").header("Authorization", "Bearer " + officialToken)
                .contentType(MediaType.APPLICATION_XML).content(resource("odluka-odobrenje-primer.xml")))
                .andExpect(status().isCreated());
        verify(resolutions).saveIfAbsent(eq("one"), anyString());
        doThrow(new IllegalStateException("TEST PDF failure")).when(pdfService).toPdf(anyString());
        mvc.perform(post(POST + "two/resenje").header("Authorization", "Bearer " + officialToken)
                .contentType(MediaType.APPLICATION_XML).content(resource("odluka-odbijanje-primer.xml")))
                .andExpect(status().isCreated());
        verify(resolutions).saveIfAbsent(eq("two"), anyString());
        verify(mailSender).send(org.mockito.ArgumentMatchers.any(MimeMessage.class));
    }

    @Test
    void ownerlessAndMissingOwnerAccountsSkipMail() throws Exception {
        request("old", null);
        mvc.perform(post(POST + "old/resenje").header("Authorization", "Bearer " + officialToken)
                .contentType(MediaType.APPLICATION_XML).content(resource("odluka-odobrenje-primer.xml")))
                .andExpect(status().isCreated());
        when(users.findById(OWNER)).thenReturn(Optional.empty());
        mvc.perform(post(POST + "one/resenje").header("Authorization", "Bearer " + officialToken)
                .contentType(MediaType.APPLICATION_XML).content(resource("odluka-odbijanje-primer.xml")))
                .andExpect(status().isCreated());
        verifyNoInteractions(mailSender, pdfService);
    }

    @Test
    void pdfReturnsDownloadAndPreservesAccessAndXmlErrors() throws Exception {
        String path = GET + "one/resenje/pdf";
        mvc.perform(get(path).accept(MediaType.APPLICATION_PDF))
                .andExpect(status().isUnauthorized()).andExpect(xpath("/greska/status").string("401"));
        mvc.perform(get(path).accept(MediaType.APPLICATION_PDF)
                .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors
                        .user("other").roles("OTHER")))
                .andExpect(status().isForbidden()).andExpect(xpath("/greska/status").string("403"));
        for (String outcome : new String[]{"odobrenje", "odbijanje"}) {
            var stored = resolutionXml.fromXml(resource("resenje-" + outcome + "-primer.xml"));
            stored.setId("http://localhost:8080/api/a1/zahtevi/one/resenje");
            stored.setZahtevRef("http://localhost:8080/api/a1/zahtevi/one");
            when(resolutions.findById("one")).thenReturn(Optional.of(resolutionXml.toXml(stored)));
            for (String token : new String[]{officialToken, citizenToken}) {
                byte[] pdf = mvc.perform(get(path).header("Authorization", "Bearer " + token)
                        .accept(MediaType.APPLICATION_PDF))
                        .andExpect(status().isOk()).andExpect(content().contentType(MediaType.APPLICATION_PDF))
                        .andExpect(header().string("Content-Disposition", "attachment; filename=\"resenje-one.pdf\""))
                        .andReturn().getResponse().getContentAsByteArray();
                assertTrue(new String(pdf, StandardCharsets.ISO_8859_1).startsWith("%PDF-"));
            }
        }
        request("foreign", "https://example.org/nalozi/other");
        request("old", null);
        for (String id : new String[]{"foreign", "old", "missing", "two"}) {
            mvc.perform(get(GET + id + "/resenje/pdf").header("Authorization", "Bearer " + citizenToken)
                    .accept(MediaType.APPLICATION_PDF))
                    .andExpect(status().isNotFound()).andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_XML))
                    .andExpect(xpath("/greska/status").string("404"));
        }
        when(resolutions.findById("one")).thenThrow(new A1RepositoryException("SECRET"));
        mvc.perform(get(path).header("Authorization", "Bearer " + officialToken).accept(MediaType.APPLICATION_PDF))
                .andExpect(status().isServiceUnavailable()).andExpect(xpath("/greska/status").string("503"));
    }

    @Test
    void xhtmlSupportsBothOutcomesAndPreservesAccessAndErrors() throws Exception {
        var xhtml = new ResenjeXhtmlServiceImpl();
        assertThrows(IllegalStateException.class, () -> xhtml.toXhtml(
                "<!DOCTYPE resenje [<!ENTITY e SYSTEM 'file:///secret'>]><resenje>&e;</resenje>"));
        mvc.perform(get(GET + "one/resenje/xhtml").accept("application/xhtml+xml"))
                .andExpect(status().isUnauthorized()).andExpect(xpath("/greska/status").string("401"));
        for (String outcome : new String[]{"odobrenje", "odbijanje"}) {
            var stored = resolutionXml.fromXml(resource("resenje-" + outcome + "-primer.xml"));
            stored.setId("http://localhost:8080/api/a1/zahtevi/one/resenje");
            stored.setZahtevRef("http://localhost:8080/api/a1/zahtevi/one");
            when(resolutions.findById("one")).thenReturn(Optional.of(resolutionXml.toXml(stored)));
            for (String token : new String[]{officialToken, citizenToken}) {
                mvc.perform(get(GET + "one/resenje/xhtml").header("Authorization", "Bearer " + token)
                        .accept("application/xhtml+xml"))
                        .andExpect(status().isOk())
                        .andExpect(content().contentType("application/xhtml+xml;charset=UTF-8"))
                        .andExpect(xpath("/h:html/h:body/h:h2[last()]",
                                java.util.Map.of("h", "http://www.w3.org/1999/xhtml"))
                                .string(outcome.equals("odobrenje") ? "Zahtev je odobren" : "Zahtev je odbijen"));
            }
        }
        request("foreign", "https://example.org/nalozi/other");
        request("old", null);
        for (String id : new String[]{"foreign", "old", "missing", "two"}) {
            mvc.perform(get(GET + id + "/resenje/xhtml").header("Authorization", "Bearer " + citizenToken)
                    .accept("application/xhtml+xml"))
                    .andExpect(status().isNotFound()).andExpect(xpath("/greska/status").string("404"));
        }
        var legacyResolution = resolutionXml.fromXml(resource("resenje-odobrenje-primer.xml"));
        legacyResolution.setId("http://localhost:8080/api/a1/zahtevi/old/resenje");
        legacyResolution.setZahtevRef("http://localhost:8080/api/a1/zahtevi/old");
        when(resolutions.findById("old")).thenReturn(Optional.of(resolutionXml.toXml(legacyResolution)));
        mvc.perform(get(GET + "old/resenje/xhtml").header("Authorization", "Bearer " + officialToken))
                .andExpect(status().isOk());
        when(resolutions.findById("one")).thenThrow(new A1RepositoryException("SECRET"));
        mvc.perform(get(GET + "one/resenje/xhtml").header("Authorization", "Bearer " + officialToken)
                .accept("application/xhtml+xml"))
                .andExpect(status().isServiceUnavailable()).andExpect(xpath("/greska/status").string("503"));
    }

    @Test
    void approvalAndRejectionUseBackendIdentityAndCanBeRead() throws Exception {
        String approved = mvc.perform(post(POST + "one/resenje").header("Authorization", "Bearer " + officialToken)
                .contentType(MediaType.APPLICATION_XML).content(resource("odluka-odobrenje-primer.xml")))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        var resolution = resolutionXml.fromXml(approved);
        assertEquals("AD-one", resolution.getOdobrenje().getSifraDela());
        assertEquals(official.getId(), resolution.getSluzbenik().getId());
        assertEquals(official.getIme(), resolution.getSluzbenik().getIme());
        assertEquals(official.getPrezime(), resolution.getSluzbenik().getPrezime());
        assertEquals(LocalDate.now().toString(), resolution.getDatumOdluke().toXMLFormat());
        assertEquals("http://localhost:8080/api/a1/zahtevi/one/resenje", resolution.getId());
        verify(resolutions).saveIfAbsent("one", approved);
        when(resolutions.findById("one")).thenReturn(Optional.of(approved));
        for (String token : new String[]{officialToken, citizenToken}) {
            mvc.perform(get(GET + "one/resenje").header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk());
        }
        String rejected = mvc.perform(post(POST + "two/resenje").header("Authorization", "Bearer " + officialToken)
                .contentType(MediaType.APPLICATION_XML).content(resource("odluka-odbijanje-primer.xml")))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertEquals("Nije dostavljen primer autorskog dela potreban za obradu zahteva.",
                resolutionXml.fromXml(rejected).getOdbijanje().getObrazlozenje());
        verify(requests, never()).save(anyString(), anyString());
    }

    @Test
    void rejectsInvalidDecisionDuplicateAndDatabaseFailure() throws Exception {
        for (String xml : new String[]{"<broken>",
                "<odluka xmlns='https://example.org/autorska-prava/odluka'><odbijanje><obrazlozenje> </obrazlozenje></odbijanje></odluka>",
                "<!DOCTYPE odluka [<!ENTITY e SYSTEM 'file:///secret'>]><odluka xmlns='https://example.org/autorska-prava/odluka'><odobrenje/></odluka>"}) {
            mvc.perform(post(POST + "one/resenje").header("Authorization", "Bearer " + officialToken)
                    .contentType(MediaType.APPLICATION_XML).content(xml))
                    .andExpect(status().isBadRequest()).andExpect(xpath("/greska/status").string("400"));
        }
        verify(resolutions, never()).saveIfAbsent(anyString(), anyString());
        doThrow(new ResenjeAlreadyExistsException()).when(resolutions).saveIfAbsent(eq("one"), anyString());
        mvc.perform(post(POST + "one/resenje").header("Authorization", "Bearer " + officialToken)
                .contentType(MediaType.APPLICATION_XML).content(resource("odluka-odobrenje-primer.xml")))
                .andExpect(status().isConflict()).andExpect(xpath("/greska/status").string("409"));
        when(resolutions.findById("one")).thenThrow(new A1RepositoryException("test"));
        mvc.perform(get(GET + "one/resenje").header("Authorization", "Bearer " + officialToken))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    void enforcesRolesOwnershipAndMissingResources() throws Exception {
        mvc.perform(get(GET + "one/resenje")).andExpect(status().isUnauthorized());
        mvc.perform(post(POST + "one/resenje").header("Authorization", "Bearer " + citizenToken)
                .contentType(MediaType.APPLICATION_XML).content(resource("odluka-odobrenje-primer.xml")))
                .andExpect(status().isForbidden());
        request("foreign", "https://example.org/nalozi/drugi");
        request("old", null);
        for (String id : new String[]{"foreign", "old", "absent", "one"}) {
            mvc.perform(get(GET + id + "/resenje").header("Authorization", "Bearer " + citizenToken))
                    .andExpect(status().isNotFound()).andExpect(xpath("/greska/status").string("404"));
        }
        verify(resolutions, never()).findById("foreign");
        verify(resolutions, never()).findById("old");
        // Officials reach resolution lookup even for ownerless documents.
        mvc.perform(get(GET + "old/resenje").header("Authorization", "Bearer " + officialToken))
                .andExpect(status().isNotFound());
        verify(resolutions).findById("old");
        mvc.perform(post(POST + "absent/resenje").header("Authorization", "Bearer " + officialToken)
                .contentType(MediaType.APPLICATION_XML).content(resource("odluka-odobrenje-primer.xml")))
                .andExpect(status().isNotFound());
        SecurityContextHolder.clearContext();
        assertThrows(AuthenticationCredentialsNotFoundException.class, () -> service.findById("one"));
        assertThrows(AuthenticationCredentialsNotFoundException.class,
                () -> service.create("one", resource("odluka-odobrenje-primer.xml")));
    }

    @Test
    void officialReadsOwnerlessResolutionAndServiceRejectsCitizenDecision() throws Exception {
        request("old", null);
        var stored = resolutionXml.fromXml(resource("resenje-odobrenje-primer.xml"));
        stored.setId("http://localhost:8080/api/a1/zahtevi/old/resenje");
        stored.setZahtevRef("http://localhost:8080/api/a1/zahtevi/old");
        when(resolutions.findById("old")).thenReturn(Optional.of(resolutionXml.toXml(stored)));
        mvc.perform(get(GET + "old/resenje").header("Authorization", "Bearer " + officialToken))
                .andExpect(status().isOk());
        SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
                OWNER, null, List.of(new SimpleGrantedAuthority("ROLE_GRADJANIN"))));
        try {
            assertThrows(AccessDeniedException.class,
                    () -> service.create("one", resource("odluka-odobrenje-primer.xml")));
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test
    void storedInvalidDocumentsAreNotMissingAndPersistedRoleIsChecked() throws Exception {
        when(resolutions.findById("one")).thenReturn(Optional.of("<broken>"));
        mvc.perform(get(GET + "one/resenje").header("Authorization", "Bearer " + officialToken))
                .andExpect(status().isServiceUnavailable());
        when(requests.findById("two")).thenReturn(Optional.of("<broken>"));
        mvc.perform(get(GET + "two/resenje").header("Authorization", "Bearer " + officialToken))
                .andExpect(status().isServiceUnavailable());
        official.setUloga(UlogaType.GRADJANIN);
        when(users.findById(official.getId())).thenReturn(Optional.of(userXml.toXml(official)));
        mvc.perform(post(POST + "one/resenje").header("Authorization", "Bearer " + officialToken)
                .contentType(MediaType.APPLICATION_XML).content(resource("odluka-odobrenje-primer.xml")))
                .andExpect(status().isForbidden());
    }

    private String resource(String name) throws Exception {
        try (var input = getClass().getResourceAsStream("/" + name)) {
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
