package com.example.xml_backend.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.xpath;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.Date;
import java.util.Optional;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import com.example.xml_backend.config.JwtConfiguration;
import com.example.xml_backend.model.korisnik.KorisnikType;
import com.example.xml_backend.model.korisnik.UlogaType;
import com.example.xml_backend.repository.KorisnikRepository;
import com.example.xml_backend.repository.KorisnikRepositoryException;
import com.example.xml_backend.service.impl.JwtTokenServiceImpl;
import com.example.xml_backend.service.impl.KorisnikXmlServiceImpl;
import com.example.xml_backend.service.impl.PrijavaOdgovorXmlServiceImpl;
import com.example.xml_backend.service.impl.PrijavaServiceImpl;
import com.example.xml_backend.service.impl.PrijavaXmlServiceImpl;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.SignatureException;
import jakarta.xml.bind.JAXBException;

class PrijavaControllerTest {
    private static final Instant NOW = Instant.parse("2026-10-23T10:00:00Z");
    private static final String PASSWORD = "  RazvojTest123!  ";
    private final KorisnikRepository repository = mock(KorisnikRepository.class);
    private final KorisnikXmlServiceImpl korisnikXml = new KorisnikXmlServiceImpl();
    private final PrijavaXmlServiceImpl prijavaXml = new PrijavaXmlServiceImpl();
    private final PrijavaOdgovorXmlServiceImpl odgovorXml = new PrijavaOdgovorXmlServiceImpl();
    private final SecretKey key = Jwts.SIG.HS256.key().build();
    private MockMvc mvc;
    private KorisnikType korisnik;
    private String example;

    @BeforeEach
    void setup() throws Exception {
        var tokens = new JwtTokenServiceImpl(key, Clock.fixed(NOW, ZoneOffset.UTC));
        var service = new PrijavaServiceImpl(prijavaXml, odgovorXml, korisnikXml, repository, tokens);
        mvc = MockMvcBuilders.standaloneSetup(new PrijavaController(service))
                .setMessageConverters(new StringHttpMessageConverter(StandardCharsets.UTF_8))
                .setControllerAdvice(new PrijavaExceptionHandler()).build();
        try (var input = getClass().getResourceAsStream("/korisnik-primer.xml")) {
            korisnik = korisnikXml.fromXml(new String(input.readAllBytes(), StandardCharsets.UTF_8));
        }
        korisnik.setLozinkaHash(BCrypt.hashpw(PASSWORD, BCrypt.gensalt(4)));
        example = "<p:prijava xmlns:p='https://example.org/autorska-prava/prijava'>"
                + "<p:email>  MILA.PRIMER@EXAMPLE.ORG  </p:email><p:lozinka>" + PASSWORD
                + "</p:lozinka></p:prijava>";
    }

    @Test
    void successfulLoginReturnsSignedTokenWithStoredRoleAnd24HourExpiry() throws Exception {
        for (UlogaType role : UlogaType.values()) {
            korisnik.setUloga(role);
            when(repository.findByEmail("mila.primer@example.org")).thenReturn(Optional.of(korisnikXml.toXml(korisnik)));
            String response = mvc.perform(post("/api/auth/prijava").contentType(MediaType.APPLICATION_XML)
                    .content(example.getBytes(StandardCharsets.UTF_8)))
                    .andExpect(status().isOk()).andExpect(header().string("Cache-Control", "no-store"))
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_XML))
                    .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
            var odgovor = odgovorXml.fromXml(response);
            var signed = Jwts.parser().verifyWith(key).clock(() -> Date.from(NOW)).build()
                    .parseSignedClaims(odgovor.getToken());
            var claims = signed.getPayload();
            assertEquals("HS256", signed.getHeader().getAlgorithm());
            assertEquals(korisnik.getId(), claims.getSubject());
            assertEquals(role.value(), claims.get("uloga", String.class));
            assertEquals(NOW, claims.getIssuedAt().toInstant());
            assertEquals(Duration.ofHours(24), Duration.between(claims.getIssuedAt().toInstant(), claims.getExpiration().toInstant()));
            assertEquals(claims.getExpiration().toInstant(), odgovor.getExpiresAt().toGregorianCalendar().toInstant());
            assertFalse(response.contains(korisnik.getLozinkaHash()));
            assertFalse(response.contains(PASSWORD));
            assertEquals(4, claims.size());
            SecretKey wrongKey = Jwts.SIG.HS256.key().build();
            assertThrows(SignatureException.class, () -> Jwts.parser().verifyWith(wrongKey)
                    .clock(() -> Date.from(NOW)).build().parseSignedClaims(odgovor.getToken()));
        }
    }

    @Test
    void missingUserAndWrongPasswordReturnIdentical401() throws Exception {
        when(repository.findByEmail("mila.primer@example.org")).thenReturn(Optional.empty());
        String missing = unauthorized(example);
        when(repository.findByEmail("mila.primer@example.org")).thenReturn(Optional.of(korisnikXml.toXml(korisnik)));
        assertEquals(missing, unauthorized(example.replace(PASSWORD, "pogresna-lozinka")));
        assertEquals(missing, unauthorized(example.replace(PASSWORD, PASSWORD.strip())));
        assertEquals(missing, unauthorized(example.replace(PASSWORD, "a".repeat(73))));
        verify(repository, org.mockito.Mockito.times(3)).findByEmail("mila.primer@example.org");
    }

    private String unauthorized(String xml) throws Exception {
        return mvc.perform(post("/api/auth/prijava").contentType(MediaType.APPLICATION_XML)
                .content(xml.getBytes(StandardCharsets.UTF_8)))
                .andExpect(status().isUnauthorized()).andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(xpath("/greska/poruka").string("Neispravan email ili lozinka."))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    @Test
    void invalidXmlAndDtdReturn400WithoutRepositoryAccess() throws Exception {
        for (String xml : new String[]{"", "<broken>", example.replace("<p:lozinka>" + PASSWORD + "</p:lozinka>", ""),
                "<!DOCTYPE prijava [<!ENTITY external SYSTEM 'file:///forbidden'>]>" + example}) {
            mvc.perform(post("/api/auth/prijava").contentType(MediaType.APPLICATION_XML)
                    .content(xml.getBytes(StandardCharsets.UTF_8)))
                    .andExpect(status().isBadRequest()).andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_XML))
                    .andExpect(xpath("/greska/poruka").string("Neispravan XML prijave."));
        }
        verifyNoInteractions(repository);
    }

    @Test
    void unavailableDatabaseReturnsXml503() throws Exception {
        when(repository.findByEmail("mila.primer@example.org")).thenThrow(new KorisnikRepositoryException());
        mvc.perform(post("/api/auth/prijava").contentType(MediaType.APPLICATION_XML)
                .content(example.getBytes(StandardCharsets.UTF_8)))
                .andExpect(status().isServiceUnavailable()).andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_XML))
                .andExpect(xpath("/greska/status").string("503"));
    }

    @Test
    void invalidStoredXmlOrHashReturnsSanitized500() throws Exception {
        for (String stored : new String[]{"<broken>", korisnikXml.toXml(korisnik).replace(korisnik.getLozinkaHash(), "invalid-hash")}) {
            when(repository.findByEmail("mila.primer@example.org")).thenReturn(Optional.of(stored));
            mvc.perform(post("/api/auth/prijava").contentType(MediaType.APPLICATION_XML)
                    .content(example.getBytes(StandardCharsets.UTF_8)))
                    .andExpect(status().isInternalServerError())
                    .andExpect(xpath("/greska/poruka").string("Interna greška pri prijavi."));
        }
    }

    @Test
    void xmlServicesPreserveWhitespaceAndValidateResponses() throws Exception {
        var login = prijavaXml.fromXml(example);
        assertEquals(PASSWORD, prijavaXml.fromXml(prijavaXml.toXml(login)).getLozinka());
        login.setEmail(null);
        assertThrows(JAXBException.class, () -> prijavaXml.toXml(login));
        var odgovor = new com.example.xml_backend.model.prijavaodgovor.PrijavaOdgovorType();
        assertThrows(JAXBException.class, () -> odgovorXml.toXml(odgovor));
    }

    @Test
    void signingConfigurationRejectsMissingInvalidAndWeakKeys() {
        var configuration = new JwtConfiguration();
        for (String invalid : new String[]{"", "not base64!", Base64.getEncoder().encodeToString(new byte[16])}) {
            assertThrows(IllegalStateException.class, () -> configuration.jwtSigningKey(invalid));
        }
        assertEquals(32, configuration.jwtSigningKey(Base64.getEncoder().encodeToString(key.getEncoded())).getEncoded().length);
    }
}
