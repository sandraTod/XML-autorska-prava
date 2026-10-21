package com.example.xml_backend.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.xpath;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import com.example.xml_backend.model.korisnik.UlogaType;
import com.example.xml_backend.repository.EmailAlreadyExistsException;
import com.example.xml_backend.repository.KorisnikRepository;
import com.example.xml_backend.repository.KorisnikRepositoryException;
import com.example.xml_backend.service.impl.KorisnikXmlServiceImpl;
import com.example.xml_backend.service.impl.RegistracijaServiceImpl;
import com.example.xml_backend.service.impl.RegistracijaXmlServiceImpl;
import jakarta.xml.bind.JAXBException;

class RegistracijaControllerTest {
    private final KorisnikRepository repository = mock(KorisnikRepository.class);
    private final RegistracijaXmlServiceImpl registrationXml = new RegistracijaXmlServiceImpl();
    private final KorisnikXmlServiceImpl userXml = new KorisnikXmlServiceImpl();
    private MockMvc mvc;
    private String example;

    @BeforeEach
    void setup() throws Exception {
        var service = new RegistracijaServiceImpl(registrationXml, userXml, repository, "https://example.org/nalozi");
        mvc = MockMvcBuilders.standaloneSetup(new RegistracijaController(service))
                .setMessageConverters(new StringHttpMessageConverter(StandardCharsets.UTF_8))
                .setControllerAdvice(new RegistracijaExceptionHandler()).build();
        try (var input = getClass().getResourceAsStream("/registracija-primer.xml")) {
            example = new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    @Test
    void registersCitizenWithNormalizedEmailAndUnchangedPassword() throws Exception {
        String password = "  test lozinka  ";
        String request = example.replace("mila.primer@example.org", "  MILA.Primer@Example.org  ")
                .replace("RazvojTest123!", password);
        mvc.perform(post("/api/auth/registracija").contentType(MediaType.APPLICATION_XML)
                .content(request.getBytes(StandardCharsets.UTF_8)))
                .andExpect(status().isCreated()).andExpect(content().string(""));
        var email = ArgumentCaptor.forClass(String.class);
        var xml = ArgumentCaptor.forClass(String.class);
        verify(repository).save(email.capture(), xml.capture());
        var user = userXml.fromXml(xml.getValue());
        assertEquals("mila.primer@example.org", email.getValue());
        assertEquals(email.getValue(), user.getEmail());
        assertEquals(UlogaType.GRADJANIN, user.getUloga());
        UUID.fromString(user.getId().substring(user.getId().lastIndexOf('/') + 1));
        assertTrue(user.getId().startsWith("https://example.org/nalozi/"));
        assertTrue(BCrypt.checkpw(password, user.getLozinkaHash()));
        assertFalse(BCrypt.checkpw(password.strip(), user.getLozinkaHash()));
        assertFalse(xml.getValue().contains(password));
        assertEquals(user.getLozinkaHash(), userXml.fromXml(userXml.toXml(user)).getLozinkaHash());
    }

    @Test
    void rejectsMalformedXmlInvalidEmailAndPasswordLimits() throws Exception {
        for (String request : new String[]{"<broken>", "", example.replace("mila.primer@example.org", "bad-email"),
                example.replace("RazvojTest123!", "1234567"), example.replace("RazvojTest123!", "a".repeat(73)),
                example.replace("RazvojTest123!", "č".repeat(37)),
                example.replace("<r:registracija ", "<r:registracija id=\"https://example.org/id\" "),
                example.replace("</r:registracija>", "<r:uloga>SLUZBENIK</r:uloga></r:registracija>")}) {
            mvc.perform(post("/api/auth/registracija").contentType(MediaType.APPLICATION_XML)
                    .content(request.getBytes(StandardCharsets.UTF_8)))
                    .andExpect(status().isBadRequest()).andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_XML))
                    .andExpect(xpath("/greska/status").string("400"));
        }
        verifyNoInteractions(repository);
    }

    @Test
    void allowsExactly72Utf8Bytes() throws Exception {
        String request = example.replace("RazvojTest123!", "č".repeat(36));
        mvc.perform(post("/api/auth/registracija").contentType(MediaType.APPLICATION_XML)
                .content(request.getBytes(StandardCharsets.UTF_8))).andExpect(status().isCreated());
    }

    @Test
    void mapsDuplicateAndDatabaseErrorsWithoutSensitiveDetails() throws Exception {
        doThrow(new EmailAlreadyExistsException()).doThrow(new KorisnikRepositoryException())
                .when(repository).save(anyString(), anyString());
        for (int expectedStatus : new int[]{409, 503}) {
            mvc.perform(post("/api/auth/registracija").contentType(MediaType.APPLICATION_XML)
                    .content(example.getBytes(StandardCharsets.UTF_8)))
                    .andExpect(status().is(expectedStatus)).andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_XML))
                    .andExpect(xpath("/greska/status").string(Integer.toString(expectedStatus)));
        }
    }

    @Test
    void xmlServicesValidateBothDirectionsAndRejectDtd() throws Exception {
        var registration = registrationXml.fromXml(example);
        assertEquals(registration.getEmail(), registrationXml.fromXml(registrationXml.toXml(registration)).getEmail());
        registration.setIme(null);
        assertThrows(JAXBException.class, () -> registrationXml.toXml(registration));
        String dtd = example.replace("?>", "?><!DOCTYPE registracija [<!ENTITY x SYSTEM 'file:///forbidden'>]>");
        assertThrows(JAXBException.class, () -> registrationXml.fromXml(dtd));
        String userExample;
        try (var input = getClass().getResourceAsStream("/korisnik-primer.xml")) {
            userExample = new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
        var user = userXml.fromXml(userExample);
        String serialized = userXml.toXml(user);
        assertEquals(serialized, userXml.toXml(userXml.fromXml(serialized)));
        String userDtd = userExample.replace("?>", "?><!DOCTYPE korisnik [<!ENTITY x SYSTEM 'file:///forbidden'>]>");
        assertThrows(JAXBException.class, () -> userXml.fromXml(userDtd));
        user.setLozinkaHash(null);
        assertThrows(JAXBException.class, () -> userXml.toXml(user));
        assertNotEquals("RazvojTest123!", serialized);
    }
}
