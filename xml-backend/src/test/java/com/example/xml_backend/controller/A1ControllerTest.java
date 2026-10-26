package com.example.xml_backend.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import com.example.xml_backend.repository.A1Repository;
import com.example.xml_backend.repository.A1RepositoryException;
import com.example.xml_backend.service.impl.A1ServiceImpl;
import com.example.xml_backend.service.impl.A1XmlServiceImpl;
import com.example.xml_backend.service.impl.A1ListaXmlServiceImpl;

class A1ControllerTest {
    private final A1Repository repository = mock(A1Repository.class);
    private final A1XmlServiceImpl xmlService = new A1XmlServiceImpl();
    private MockMvc mvc;
    private String example;

    @BeforeEach
    void setup() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
                "https://example.org/nalozi/test", null, List.of(new SimpleGrantedAuthority("ROLE_GRADJANIN"))));
        var service = new A1ServiceImpl(xmlService, repository, new A1ListaXmlServiceImpl(), "http://localhost:8080/api/a1/zahtevi");
        mvc = MockMvcBuilders.standaloneSetup(new A1Controller(service))
                .setMessageConverters(new org.springframework.http.converter.StringHttpMessageConverter(StandardCharsets.UTF_8))
                .setControllerAdvice(new A1ExceptionHandler()).build();
        try (var input = getClass().getResourceAsStream("/a1-zahtev-primer.xml")) {
            example = new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createsAndRetrievesSavedDocument() throws Exception {
        var response = mvc.perform(post("/api/a1/zahtevi").contentType(MediaType.APPLICATION_XML)
                .content(example.getBytes(StandardCharsets.UTF_8)))
                .andExpect(status().isCreated()).andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_XML))
                .andReturn().getResponse();
        var id = ArgumentCaptor.forClass(String.class);
        var xml = ArgumentCaptor.forClass(String.class);
        verify(repository).save(id.capture(), xml.capture());
        assertEquals(id.getValue(), UUID.fromString(id.getValue()).toString());
        assertEquals("http://localhost:8080/api/a1/zahtevi/" + id.getValue(), response.getHeader("Location"));
        var saved = xmlService.fromXml(xml.getValue());
        assertEquals(response.getHeader("Location"), saved.getId());
        assertEquals("https://example.org/nalozi/test", saved.getVlasnikId());
        assertEquals("Priče sa zamišljene obale", saved.getPodaciGradjanina().getDelo().getNaslov());
        assertEquals(xml.getValue(), response.getContentAsString(StandardCharsets.UTF_8));
        when(repository.findById(id.getValue())).thenReturn(Optional.of(xml.getValue()));
        mvc.perform(get("/api/a1/zahtevi/" + id.getValue()))
                .andExpect(status().isOk()).andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_XML))
                .andExpect(content().string(xml.getValue()));
    }

    @Test
    void rejectsInvalidXmlAndClientOwnedFields() throws Exception {
        String clientId = example.replace("xsi:schemaLocation=", "id=\"https://example.org/client-id\" xsi:schemaLocation=");
        String official = example.replace("</a1:zahtevA1>", "<a1:sluzbeniDeo><a1:brojPrijave>A-1</a1:brojPrijave>"
                + "<a1:datumPodnosenja>2026-10-14</a1:datumPodnosenja></a1:sluzbeniDeo></a1:zahtevA1>");
        for (String input : new String[]{"<broken>", example.replace("<a1:naslov>Priče sa zamišljene obale</a1:naslov>", ""), ""}) {
            mvc.perform(post("/api/a1/zahtevi").contentType(MediaType.APPLICATION_XML)
                    .content(input.getBytes(StandardCharsets.UTF_8)))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_XML))
                    .andExpect(xpath("/greska/status").string("400"))
                    .andExpect(xpath("/greska/poruka").string("Neispravan A-1 XML."));
        }
        String clientOwner = example.replace("xsi:schemaLocation=", "vlasnikId=\"https://example.org/nalozi/test\" xsi:schemaLocation=");
        for (String input : new String[]{clientId, official, clientOwner}) {
            mvc.perform(post("/api/a1/zahtevi").contentType(MediaType.APPLICATION_XML)
                    .content(input.getBytes(StandardCharsets.UTF_8)))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_XML))
                    .andExpect(xpath("/greska/status").string("400"))
                    .andExpect(xpath("/greska/poruka").string("Unos polja id, vlasnikId i sluzbeniDeo nije dozvoljen."));
        }
        verifyNoInteractions(repository);
    }

    @Test
    void missingDocumentReturnsXml404() throws Exception {
        when(repository.findById("missing")).thenReturn(Optional.empty());
        mvc.perform(get("/api/a1/zahtevi/missing")).andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_XML))
                .andExpect(xpath("/greska/status").string("404"));
    }

    @Test
    void databaseErrorsReturnSanitizedXml503() throws Exception {
        var failure = new A1RepositoryException("SECRET internal details");
        when(repository.findById(anyString())).thenThrow(failure);
        doThrow(failure).when(repository).save(anyString(), anyString());
        for (var request : new org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder[]{
                get("/api/a1/zahtevi/test"), post("/api/a1/zahtevi")
                        .contentType(MediaType.APPLICATION_XML).content(example.getBytes(StandardCharsets.UTF_8))}) {
            var response = mvc.perform(request).andExpect(status().isServiceUnavailable())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_XML))
                    .andExpect(xpath("/greska/status").string("503")).andReturn().getResponse();
            assertFalse(response.getContentAsString().contains("SECRET"));
        }
    }
}
