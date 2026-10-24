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
import com.example.xml_backend.service.KorisnikXmlService;
import com.example.xml_backend.service.PrijavaOdgovorXmlService;
import io.jsonwebtoken.Jwts;

@SpringBootTest
class JwtSecurityTest {
    private static final SecretKey TEST_KEY = Jwts.SIG.HS512.key().build();
    @DynamicPropertySource
    static void testProperties(DynamicPropertyRegistry properties) {
        properties.add("jwt.secret-base64", () -> Base64.getEncoder().encodeToString(TEST_KEY.getEncoded()));
    }

    @Autowired private WebApplicationContext context;
    @Autowired private JwtTokenService tokens;
    @Autowired private KorisnikXmlService userXml;
    @Autowired private PrijavaOdgovorXmlService responseXml;
    @Autowired private SecretKey signingKey;
    @Autowired private SecurityFilterChain securityChain;
    @MockitoBean private A1Repository a1Repository;
    @MockitoBean private KorisnikRepository userRepository;
    private MockMvc mvc;
    private String example;
    private KorisnikType user;

    @BeforeEach
    void setup() throws Exception {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        example = resource("a1-zahtev-primer.xml");
        user = userXml.fromXml(resource("korisnik-primer.xml"));
    }

    private String resource(String name) throws Exception {
        try (var input = getClass().getResourceAsStream("/" + name)) {
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
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
        when(a1Repository.findById("test")).thenReturn(Optional.of(example));
        for (UlogaType role : UlogaType.values()) {
            mvc.perform(get("/api/a1/zahtevi/test").header("Authorization", bearer(role)))
                    .andExpect(status().isOk()).andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_XML))
                    .andExpect(content().string(example));
        }
        mvc.perform(post("/api/a1/zahtevi").header("Authorization", bearer(UlogaType.SLUZBENIK))
                .contentType(MediaType.APPLICATION_XML).content(example.getBytes(StandardCharsets.UTF_8)))
                .andExpect(status().isForbidden()).andExpect(xpath("/greska/status").string("403"));
        var created = mvc.perform(post("/api/a1/zahtevi").header("Authorization", bearer(UlogaType.GRADJANIN))
                .contentType(MediaType.APPLICATION_XML).content(example.getBytes(StandardCharsets.UTF_8)))
                .andExpect(status().isCreated()).andExpect(header().exists("Location"))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_XML)).andReturn();
        assertNull(created.getRequest().getSession(false));
        // Autentifikacija se ne prenosi sa prethodnog zahteva bez Bearer zaglavlja.
        mvc.perform(get("/api/a1/zahtevi/test")).andExpect(status().isUnauthorized());
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
}
