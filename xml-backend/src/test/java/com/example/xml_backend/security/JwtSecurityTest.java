package com.example.xml_backend.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.xpath;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import com.example.xml_backend.model.korisnik.KorisnikType;
import com.example.xml_backend.model.korisnik.UlogaType;
import com.example.xml_backend.repository.A1Repository;
import com.example.xml_backend.repository.KorisnikRepository;
import com.example.xml_backend.service.JwtTokenService;
import com.example.xml_backend.service.A1XmlService;
import com.example.xml_backend.service.A1ListaXmlService;
import com.example.xml_backend.model.a1.ListaZahtevaType;
import com.example.xml_backend.model.a1.ZahtevA1Type;
import com.example.xml_backend.repository.A1RepositoryException;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.doReturn;
import com.example.xml_backend.service.KorisnikXmlService;
import com.example.xml_backend.service.PrijavaOdgovorXmlService;
import io.jsonwebtoken.Jwts;
import com.example.xml_backend.service.EmailNormalizer;
import com.example.xml_backend.service.A1PretragaXmlService;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

@SpringBootTest
class JwtSecurityTest {
    private static final SecretKey TEST_KEY = Jwts.SIG.HS512.key().build();
    @DynamicPropertySource
    static void testProperties(DynamicPropertyRegistry properties) {
        properties.add("jwt.secret-base64", () -> Base64.getEncoder().encodeToString(TEST_KEY.getEncoded()));
    }

    @Autowired private WebApplicationContext context;
    @Autowired private JwtTokenService tokens;
    @Autowired private A1XmlService a1Xml;
    @Autowired private A1ListaXmlService listXml;
    @Autowired private A1PretragaXmlService searchXml;
    @Autowired private KorisnikXmlService userXml;
    @Autowired private PrijavaOdgovorXmlService responseXml;
    @Autowired private SecretKey signingKey;
    @Autowired private SecurityFilterChain securityChain;
    @MockitoBean private A1Repository a1Repository;
    @MockitoBean private KorisnikRepository userRepository;
    private MockMvc mvc;
    private String example;
    private KorisnikType user;

    @Test
    void acceptedSearchAllowsBothRolesAndReturnsOnlyTheValidatedProjection() throws Exception {
        String query = "  PriČE\t sa   obale ";
        when(a1Repository.searchAccepted(query)).thenReturn(resource("a1-pretraga-primer.xml"));
        for (UlogaType role : new UlogaType[]{UlogaType.GRADJANIN, UlogaType.SLUZBENIK}) {
            user.setUloga(role);
            String response = mvc.perform(get("/api/a1/zavedena-dela").param("q", query)
                    .header("Authorization", "Bearer " + tokens.issue(user).token()))
                    .andExpect(status().isOk()).andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_XML))
                    .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
            assertEquals(2, searchXml.fromXml(response).getDelo().size());
            assertFalse(response.contains("podnosilac"));
            assertFalse(response.contains("vlasnikId"));
            assertFalse(response.contains("potpis"));
        }
        verify(a1Repository, org.mockito.Mockito.times(2)).searchAccepted(query);
        when(a1Repository.searchAccepted("")).thenReturn("<rezultatiPretrage xmlns='https://example.org/autorska-prava/a1-pretraga'/>");
        mvc.perform(get("/api/a1/zavedena-dela").header("Authorization", "Bearer " + tokens.issue(user).token()))
                .andExpect(status().isOk());
        mvc.perform(get("/api/a1/zavedena-dela")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/a1/zavedena-dela").with(user("other").roles("OTHER"))).andExpect(status().isForbidden());
        when(a1Repository.searchAccepted("error")).thenThrow(new A1RepositoryException("test"));
        when(a1Repository.searchAccepted("invalid")).thenReturn("<broken>");
        for (String q : new String[]{"error", "invalid"}) {
            mvc.perform(get("/api/a1/zavedena-dela").param("q", q)
                    .header("Authorization", "Bearer " + tokens.issue(user).token()))
                    .andExpect(status().isServiceUnavailable()).andExpect(xpath("/greska/status").string("503"));
        }
    }

    @BeforeEach
    void setup() throws Exception {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        example = resource("a1-zahtev-primer.xml");
        user = userXml.fromXml(resource("korisnik-primer.xml"));
    }

    @Test
    void xhtmlUsesExistingOwnershipAndXmlErrors() throws Exception {
        var document = a1Xml.fromXml(example);
        document.setVlasnikId(user.getId());
        when(a1Repository.findById("owned")).thenReturn(Optional.of(a1Xml.toXml(document)));
        when(a1Repository.findById("legacy")).thenReturn(Optional.of(example));
        document.setVlasnikId("https://example.org/nalozi/other");
        when(a1Repository.findById("foreign")).thenReturn(Optional.of(a1Xml.toXml(document)));
        String path = "/api/a1/zahtevi/owned/xhtml";
        mvc.perform(get(path).accept("application/xhtml+xml"))
                .andExpect(status().isUnauthorized()).andExpect(xpath("/greska/status").string("401"));
        mvc.perform(get(path).with(user("other").roles("OTHER")).accept("application/xhtml+xml"))
                .andExpect(status().isForbidden()).andExpect(xpath("/greska/status").string("403"));
        mvc.perform(get(path).header("Authorization", bearer(UlogaType.GRADJANIN)).accept("application/xhtml+xml"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/xhtml+xml;charset=UTF-8"))
                .andExpect(xpath("/h:html", java.util.Map.of("h", "http://www.w3.org/1999/xhtml")).exists());
        for (String id : new String[]{"foreign", "legacy", "missing"}) {
            mvc.perform(get("/api/a1/zahtevi/" + id + "/xhtml")
                    .header("Authorization", bearer(UlogaType.GRADJANIN)).accept("application/xhtml+xml"))
                    .andExpect(status().isNotFound()).andExpect(xpath("/greska/status").string("404"));
        }
        for (String id : new String[]{"owned", "foreign", "legacy"}) {
            mvc.perform(get("/api/a1/zahtevi/" + id + "/xhtml")
                    .header("Authorization", bearer(UlogaType.SLUZBENIK)).accept("application/xhtml+xml"))
                    .andExpect(status().isOk()).andExpect(content().contentTypeCompatibleWith("application/xhtml+xml"));
        }
        when(a1Repository.findById("error")).thenThrow(new A1RepositoryException("SECRET"));
        mvc.perform(get("/api/a1/zahtevi/error/xhtml").header("Authorization", bearer(UlogaType.SLUZBENIK))
                .accept("application/xhtml+xml"))
                .andExpect(status().isServiceUnavailable()).andExpect(xpath("/greska/status").string("503"));
    }

    private String resource(String name) throws Exception {
        try (var input = getClass().getResourceAsStream("/" + name)) {
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    @Test
    void pdfUsesExistingOwnershipAndXmlErrors() throws Exception {
        var document = a1Xml.fromXml(example);
        document.setVlasnikId(user.getId());
        when(a1Repository.findById("owned")).thenReturn(Optional.of(a1Xml.toXml(document)));
        when(a1Repository.findById("legacy")).thenReturn(Optional.of(example));
        document.setVlasnikId("https://example.org/nalozi/other");
        when(a1Repository.findById("foreign")).thenReturn(Optional.of(a1Xml.toXml(document)));
        String path = "/api/a1/zahtevi/owned/pdf";
        mvc.perform(get(path).accept(MediaType.APPLICATION_PDF))
                .andExpect(status().isUnauthorized()).andExpect(xpath("/greska/status").string("401"));
        mvc.perform(get(path).with(user("other").roles("OTHER")).accept(MediaType.APPLICATION_PDF))
                .andExpect(status().isForbidden()).andExpect(xpath("/greska/status").string("403"));
        byte[] pdf = mvc.perform(get(path).header("Authorization", bearer(UlogaType.GRADJANIN))
                .accept(MediaType.APPLICATION_PDF)).andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"zahtev-owned.pdf\""))
                .andReturn().getResponse().getContentAsByteArray();
        assertTrue(new String(pdf, StandardCharsets.ISO_8859_1).startsWith("%PDF-"));
        for (String id : new String[]{"foreign", "legacy", "missing"}) {
            mvc.perform(get("/api/a1/zahtevi/" + id + "/pdf")
                    .header("Authorization", bearer(UlogaType.GRADJANIN)).accept(MediaType.APPLICATION_PDF))
                    .andExpect(status().isNotFound()).andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_XML))
                    .andExpect(xpath("/greska/status").string("404"));
        }
        for (String id : new String[]{"owned", "foreign", "legacy"}) {
            mvc.perform(get("/api/a1/zahtevi/" + id + "/pdf")
                    .header("Authorization", bearer(UlogaType.SLUZBENIK)).accept(MediaType.APPLICATION_PDF))
                    .andExpect(status().isOk()).andExpect(content().contentType(MediaType.APPLICATION_PDF));
        }
        when(a1Repository.findById("error")).thenThrow(new A1RepositoryException("SECRET"));
        mvc.perform(get("/api/a1/zahtevi/error/pdf").header("Authorization", bearer(UlogaType.SLUZBENIK))
                .accept(MediaType.APPLICATION_PDF))
                .andExpect(status().isServiceUnavailable()).andExpect(xpath("/greska/status").string("503"));
    }

    private String bearer(UlogaType role) {
        user.setUloga(role);
        return "Bearer " + tokens.issue(user).token();
    }

    @Test
    void publicRegistrationAndLoginWorkThroughRealSecurityChain() throws Exception {
        mvc.perform(post("/api/auth/registracija").contentType(MediaType.APPLICATION_XML)
                .content(resource("registracija-primer.xml").getBytes(StandardCharsets.UTF_8)))
                .andExpect(status().isCreated()).andExpect(content().string(""));
        user.setLozinkaHash(BCrypt.hashpw("RazvojTest123!", BCrypt.gensalt(4)));
        when(userRepository.findByEmail("mila.primer@example.org")).thenReturn(Optional.of(userXml.toXml(user)));
        var login = mvc.perform(post("/api/auth/prijava").contentType(MediaType.APPLICATION_XML)
                .content(resource("prijava-primer.xml").getBytes(StandardCharsets.UTF_8)))
                .andExpect(status().isOk()).andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_XML)).andReturn();
        String token = responseXml.fromXml(login.getResponse().getContentAsString(StandardCharsets.UTF_8)).getToken();
        assertEquals(user.getId(), tokens.verify(token).subject());
        assertNull(login.getRequest().getSession(false));
        assertNull(login.getResponse().getHeader("Set-Cookie"));
        mvc.perform(get("/api/a1/zahtevi/test").header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound()).andExpect(xpath("/greska/status").string("404"));
    }

    @Test
    void protectedRoutesRequireBearerTokenAndIgnoreCookies() throws Exception {
        mvc.perform(get("/api/a1/zahtevi/test")).andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", "Bearer"))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_XML))
                .andExpect(xpath("/greska/status").string("401"));
        mvc.perform(post("/api/a1/zahtevi").contentType(MediaType.APPLICATION_XML).content(example))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/a1/zahtevi/test").header("Cookie", "token=" + bearer(UlogaType.GRADJANIN)))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(a1Repository);
    }

    @Test
    void invalidExpiredUnsignedWrongAlgorithmOrClaimsReturn401() throws Exception {
        Instant now = Instant.now();
        String valid = tokens.issue(user).token();
        String[] invalid = {
                "bad-token", "", valid.substring(0, valid.lastIndexOf('.') + 1) + "AAAA",
                Jwts.builder().subject(user.getId()).claim("uloga", "GRADJANIN").expiration(Date.from(now.plusSeconds(60)))
                        .signWith(Jwts.SIG.HS256.key().build(), Jwts.SIG.HS256).compact(),
                signed(user.getId(), "GRADJANIN", now.minusSeconds(1)),
                signed(user.getId(), "GRADJANIN", null), signed(null, "GRADJANIN", now.plusSeconds(60)),
                signed("not-a-url", "GRADJANIN", now.plusSeconds(60)),
                signed(user.getId(), "ADMIN", now.plusSeconds(60)), signed(user.getId(), null, now.plusSeconds(60)),
                Jwts.builder().subject(user.getId()).claim("uloga", "GRADJANIN").expiration(Date.from(now.plusSeconds(60)))
                        .signWith(signingKey, Jwts.SIG.HS512).compact(),
                Jwts.builder().subject(user.getId()).claim("uloga", "GRADJANIN").expiration(Date.from(now.plusSeconds(60))).compact()
        };
        for (String token : invalid) {
            mvc.perform(get("/api/a1/zahtevi/test").header("Authorization", "Bearer " + token))
                    .andExpect(status().isUnauthorized()).andExpect(xpath("/greska/status").string("401"))
                    .andExpect(xpath("/greska/poruka").string("Potrebna je važeća prijava."));
        }
        mvc.perform(get("/api/a1/zahtevi/test").header("Authorization", "Basic dXNlcjpwYXNz"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(a1Repository);
    }

    private String signed(String subject, String role, Instant expiry) {
        return Jwts.builder().subject(subject).claim("uloga", role)
                .expiration(expiry == null ? null : Date.from(expiry)).signWith(signingKey, Jwts.SIG.HS256).compact();
    }

    @Test
    void citizenCanPostWhileOfficialCannotAndBothCanRead() throws Exception {
        String owned = example.replace("xsi:schemaLocation=", "vlasnikId=\"" + user.getId() + "\" xsi:schemaLocation=");
        when(a1Repository.findById("test")).thenReturn(Optional.of(owned));
        for (UlogaType role : UlogaType.values()) {
            mvc.perform(get("/api/a1/zahtevi/test").header("Authorization", bearer(role)))
                    .andExpect(status().isOk()).andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_XML))
                    .andExpect(content().string(owned));
        }
        mvc.perform(post("/api/a1/zahtevi").header("Authorization", bearer(UlogaType.SLUZBENIK))
                .contentType(MediaType.APPLICATION_XML).content(example.getBytes(StandardCharsets.UTF_8)))
                .andExpect(status().isForbidden()).andExpect(xpath("/greska/status").string("403"));
        var created = mvc.perform(post("/api/a1/zahtevi").header("Authorization", bearer(UlogaType.GRADJANIN))
                .contentType(MediaType.APPLICATION_XML).content(example.getBytes(StandardCharsets.UTF_8)))
                .andExpect(status().isCreated()).andExpect(header().exists("Location"))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_XML)).andReturn();
        assertNull(created.getRequest().getSession(false));
        var saved = a1Xml.fromXml(created.getResponse().getContentAsString(StandardCharsets.UTF_8));
        assertEquals(user.getId(), saved.getVlasnikId());
        // Autentifikacija se ne prenosi sa prethodnog zahteva bez Bearer zaglavlja.
        mvc.perform(get("/api/a1/zahtevi/test")).andExpect(status().isUnauthorized());
    }

    @Test
    void ownershipAndLegacyAccessAreCheckedThroughSecurityChain() throws Exception {
        String owned = example.replace("xsi:schemaLocation=", "vlasnikId=\"" + user.getId() + "\" xsi:schemaLocation=");
        String foreign = example.replace("xsi:schemaLocation=", "vlasnikId=\"https://example.org/nalozi/drugi\" xsi:schemaLocation=");
        when(a1Repository.findById("owned")).thenReturn(Optional.of(owned));
        when(a1Repository.findById("foreign")).thenReturn(Optional.of(foreign));
        when(a1Repository.findById("legacy")).thenReturn(Optional.of(example));
        mvc.perform(get("/api/a1/zahtevi/owned").header("Authorization", bearer(UlogaType.GRADJANIN)))
                .andExpect(status().isOk()).andExpect(content().string(owned));
        for (String document : new String[]{"foreign", "legacy"}) {
            mvc.perform(get("/api/a1/zahtevi/" + document).header("Authorization", bearer(UlogaType.GRADJANIN)))
                    .andExpect(status().isNotFound()).andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_XML))
                    .andExpect(xpath("/greska/status").string("404"));
        }
        for (String document : new String[]{"owned", "foreign", "legacy"}) {
            mvc.perform(get("/api/a1/zahtevi/" + document).header("Authorization", bearer(UlogaType.SLUZBENIK)))
                    .andExpect(status().isOk()).andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_XML));
        }
        mvc.perform(post("/api/a1/zahtevi").header("Authorization", bearer(UlogaType.GRADJANIN))
                .contentType(MediaType.APPLICATION_XML).content(owned.getBytes(StandardCharsets.UTF_8)))
                .andExpect(status().isBadRequest()).andExpect(xpath("/greska/status").string("400"));
    }

    @Test
    void otherRoutesAreDeniedAndXmlBusinessErrorsArePreserved() throws Exception {
        mvc.perform(put("/api/a1/zahtevi/test").header("Authorization", bearer(UlogaType.GRADJANIN)))
                .andExpect(status().isForbidden()).andExpect(xpath("/greska/status").string("403"));
        mvc.perform(get("/api/auth/prijava").header("Authorization", bearer(UlogaType.GRADJANIN)))
                .andExpect(status().isForbidden());
        mvc.perform(get("/not-allowed").header("Authorization", bearer(UlogaType.GRADJANIN)))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/a1/zahtevi").header("Authorization", bearer(UlogaType.GRADJANIN))
                .contentType(MediaType.APPLICATION_XML).content("<broken>"))
                .andExpect(status().isBadRequest()).andExpect(xpath("/greska/status").string("400"));
        mvc.perform(post("/api/auth/prijava").contentType(MediaType.APPLICATION_XML).content("<broken>"))
                .andExpect(status().isBadRequest()).andExpect(xpath("/greska/status").string("400"));
    }

    @Test
    void filterExistsOnlyOnceInsideSecurityChain() {
        assertEquals(1, securityChain.getFilters().stream().filter(JwtAuthenticationFilter.class::isInstance).count());
        assertTrue(context.getBeansOfType(JwtAuthenticationFilter.class).isEmpty());
        assertTrue(context.getBeansOfType(UserDetailsService.class).isEmpty());
        assertFalse(securityChain.getFilters().stream().anyMatch(filter -> filter.getClass().getSimpleName().equals("BasicAuthenticationFilter")));
    }

    @Test
    void myRequestsReturnsValidatedRepositoryResults() throws Exception {
        String owner = user.getId();
        var list = new ListaZahtevaType();
        for (String id : new String[]{"b", "a"}) {
            var document = a1Xml.fromXml(example);
            document.setId("https://example.org/a1/" + id);
            document.setVlasnikId(owner);
            list.getZahtevA1().add(document);
        }
        when(a1Repository.findByOwner(owner)).thenReturn(listXml.toXml(list));
        var response = mvc.perform(get("/api/a1/zahtevi/listaZahteva").header("Authorization", bearer(UlogaType.GRADJANIN)))
                .andExpect(status().isOk()).andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_XML))
                .andReturn().getResponse();
        var result = listXml.fromXml(response.getContentAsString(StandardCharsets.UTF_8));
        assertEquals(2, result.getZahtevA1().size());
        assertEquals(Set.of("https://example.org/a1/a", "https://example.org/a1/b"),
                result.getZahtevA1().stream().map(ZahtevA1Type::getId).collect(Collectors.toSet()));
        assertTrue(result.getZahtevA1().stream().allMatch(document -> owner.equals(document.getVlasnikId())));
        verify(a1Repository).findByOwner(owner);
    }

    @Test
    void myRequestsCanBeEmptyAndDoesNotAcceptClientOwner() throws Exception {
        when(a1Repository.findByOwner(user.getId())).thenReturn(listXml.toXml(new ListaZahtevaType()));
        var response = mvc.perform(get("/api/a1/zahtevi/listaZahteva").header("Authorization", bearer(UlogaType.GRADJANIN)))
                .andExpect(status().isOk()).andReturn().getResponse();
        assertTrue(listXml.fromXml(response.getContentAsString(StandardCharsets.UTF_8)).getZahtevA1().isEmpty());
        mvc.perform(get("/api/a1/zahtevi/listaZahteva").param("vlasnikId", "https://example.org/other")
                .header("Authorization", bearer(UlogaType.GRADJANIN)))
                .andExpect(status().isBadRequest()).andExpect(xpath("/greska/status").string("400"));
    }

    @Test
    void myRequestsRequiresCitizenAndPreservesDatabaseErrors() throws Exception {
        mvc.perform(get("/api/a1/zahtevi/listaZahteva")).andExpect(status().isUnauthorized())
                .andExpect(xpath("/greska/status").string("401"));
        mvc.perform(get("/api/a1/zahtevi/listaZahteva").header("Authorization", bearer(UlogaType.SLUZBENIK)))
                .andExpect(status().isForbidden()).andExpect(xpath("/greska/status").string("403"));
        verifyNoInteractions(a1Repository);
        when(a1Repository.findByOwner(user.getId())).thenThrow(new A1RepositoryException("Internal details"));
        mvc.perform(get("/api/a1/zahtevi/listaZahteva").header("Authorization", bearer(UlogaType.GRADJANIN)))
                .andExpect(status().isServiceUnavailable()).andExpect(xpath("/greska/status").string("503"));
        doReturn("<broken>").when(a1Repository).findByOwner(user.getId());
        mvc.perform(get("/api/a1/zahtevi/listaZahteva").header("Authorization", bearer(UlogaType.GRADJANIN)))
                .andExpect(status().isServiceUnavailable()).andExpect(xpath("/greska/status").string("503"));
    }

    @Test
    void predefinedOfficialCanLoginListAllAndReadLegacyDocument() throws Exception {
        String officialXml = resource("sluzbenik-primer.xml");
        var official = userXml.fromXml(officialXml);
        assertEquals(UlogaType.SLUZBENIK, official.getUloga());
        assertEquals(official.getEmail(), EmailNormalizer.normalize(official.getEmail()));
        assertTrue(BCrypt.checkpw("SluzbenikTest123!", official.getLozinkaHash()));
        when(userRepository.findByEmail(official.getEmail())).thenReturn(Optional.of(officialXml));
        String loginXml = "<p:prijava xmlns:p='https://example.org/autorska-prava/prijava'>"
                + "<p:email>SLUZBENIK.RAZVOJ@EXAMPLE.ORG</p:email><p:lozinka>SluzbenikTest123!</p:lozinka></p:prijava>";
        var login = mvc.perform(post("/api/auth/prijava").contentType(MediaType.APPLICATION_XML)
                .content(loginXml.getBytes(StandardCharsets.UTF_8))).andExpect(status().isOk()).andReturn();
        String token = responseXml.fromXml(login.getResponse().getContentAsString(StandardCharsets.UTF_8)).getToken();
        assertEquals(UlogaType.SLUZBENIK, tokens.verify(token).uloga());
        assertEquals(official.getId(), tokens.verify(token).subject());
        var documents = new ListaZahtevaType();
        for (String owner : new String[]{user.getId(), "https://example.org/nalozi/other", null}) {
            var document = a1Xml.fromXml(example);
            document.setVlasnikId(owner);
            documents.getZahtevA1().add(document);
        }
        when(a1Repository.findAll()).thenReturn(listXml.toXml(documents));
        var response = mvc.perform(get("/api/sluzbenik/a1/zahtevi").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_XML))
                .andReturn().getResponse();
        var result = listXml.fromXml(response.getContentAsString(StandardCharsets.UTF_8));
        assertEquals(3, result.getZahtevA1().size());
        assertEquals(1, result.getZahtevA1().stream().filter(document -> document.getVlasnikId() == null).count());
        when(a1Repository.findById("legacy")).thenReturn(Optional.of(example));
        mvc.perform(get("/api/a1/zahtevi/legacy").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(content().string(example));
    }

    @Test
    void officialListRequiresOfficialAndReturnsEmptyOr503() throws Exception {
        mvc.perform(get("/api/sluzbenik/a1/zahtevi")).andExpect(status().isUnauthorized())
                .andExpect(xpath("/greska/status").string("401"));
        mvc.perform(get("/api/sluzbenik/a1/zahtevi").header("Authorization", bearer(UlogaType.GRADJANIN)))
                .andExpect(status().isForbidden()).andExpect(xpath("/greska/status").string("403"));
        verifyNoInteractions(a1Repository);
        when(a1Repository.findAll()).thenReturn(listXml.toXml(new ListaZahtevaType()));
        var response = mvc.perform(get("/api/sluzbenik/a1/zahtevi").header("Authorization", bearer(UlogaType.SLUZBENIK)))
                .andExpect(status().isOk()).andReturn().getResponse();
        assertTrue(listXml.fromXml(response.getContentAsString(StandardCharsets.UTF_8)).getZahtevA1().isEmpty());
        when(a1Repository.findAll()).thenThrow(new A1RepositoryException("Internal details"));
        mvc.perform(get("/api/sluzbenik/a1/zahtevi").header("Authorization", bearer(UlogaType.SLUZBENIK)))
                .andExpect(status().isServiceUnavailable()).andExpect(xpath("/greska/status").string("503"));
        doReturn("<broken>").when(a1Repository).findAll();
        mvc.perform(get("/api/sluzbenik/a1/zahtevi").header("Authorization", bearer(UlogaType.SLUZBENIK)))
                .andExpect(status().isServiceUnavailable()).andExpect(xpath("/greska/status").string("503"));
    }
}
