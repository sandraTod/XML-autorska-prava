package com.example.xml_backend.service.impl;

import java.net.URI;
import java.time.LocalDate;
import javax.xml.datatype.DatatypeFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.example.xml_backend.model.a1.ZahtevA1Type;
import com.example.xml_backend.model.korisnik.KorisnikType;
import com.example.xml_backend.model.korisnik.UlogaType;
import com.example.xml_backend.model.odluka.OdlukaType;
import com.example.xml_backend.model.resenje.ResenjeType;
import com.example.xml_backend.model.resenje.SluzbenikType;
import com.example.xml_backend.model.resenje.OdobrenjeType;
import com.example.xml_backend.model.resenje.OdbijanjeType;
import com.example.xml_backend.repository.A1Repository;
import com.example.xml_backend.repository.A1RepositoryException;
import com.example.xml_backend.repository.KorisnikRepository;
import com.example.xml_backend.repository.ResenjeRepository;
import com.example.xml_backend.service.A1XmlService;
import com.example.xml_backend.service.KorisnikXmlService;
import com.example.xml_backend.service.OdlukaXmlService;
import com.example.xml_backend.service.ResenjeXmlService;
import com.example.xml_backend.service.ResenjeService;
import com.example.xml_backend.service.ResenjePdfService;
import com.example.xml_backend.service.EmailService;
import com.example.xml_backend.service.exception.InvalidOdlukaXmlException;
import com.example.xml_backend.repository.ResenjeNotFoundException;
import jakarta.xml.bind.JAXBException;

@Service
public class ResenjeServiceImpl implements ResenjeService {
    private static final Logger logger = LoggerFactory.getLogger(ResenjeServiceImpl.class);
    private final A1Repository requests;
    private final KorisnikRepository users;
    private final ResenjeRepository repository;
    private final A1XmlService requestXml;
    private final KorisnikXmlService userXml;
    private final OdlukaXmlService decisionXml;
    private final ResenjeXmlService resolutionXml;
    private final ResenjePdfService resolutionPdf;
    private final EmailService emailService;
    private final String documentBaseUrl;

    public ResenjeServiceImpl(A1Repository requests, KorisnikRepository users, ResenjeRepository repository,
            A1XmlService requestXml, KorisnikXmlService userXml, OdlukaXmlService decisionXml,
            ResenjeXmlService resolutionXml, ResenjePdfService resolutionPdf, EmailService emailService,
            @Value("${a1.document-base-url}") String documentBaseUrl) {
        this.requests = requests;
        this.users = users;
        this.repository = repository;
        this.requestXml = requestXml;
        this.userXml = userXml;
        this.decisionXml = decisionXml;
        this.resolutionXml = resolutionXml;
        this.resolutionPdf = resolutionPdf;
        this.emailService = emailService;
        URI base = URI.create(documentBaseUrl);
        if (!("http".equals(base.getScheme()) || "https".equals(base.getScheme()))
                || base.getHost() == null || base.getUserInfo() != null
                || base.getQuery() != null || base.getFragment() != null) {
            throw new IllegalArgumentException("Neispravna osnova URL identifikatora");
        }
        this.documentBaseUrl = documentBaseUrl.replaceAll("/+$", "");
    }

    @Override
    public CreatedDocument create(String documentId, String xml) {
        Authentication authentication = requireRole("ROLE_SLUZBENIK");
        OdlukaType decision;
        try {
            if (xml == null || xml.isBlank()) throw new InvalidOdlukaXmlException();
            decision = decisionXml.fromXml(xml);
        } catch (JAXBException exception) {
            throw new InvalidOdlukaXmlException();
        }
        ZahtevA1Type request = readRequest(documentId);
        KorisnikType official;
        try {
            official = userXml.fromXml(users.findById(authentication.getName())
                    .orElseThrow(() -> new AccessDeniedException("Sluzbenik nije dostupan")));
        } catch (JAXBException exception) {
            throw new A1RepositoryException("Neispravan sacuvani korisnicki dokument");
        }
        if (!authentication.getName().equals(official.getId())) {
            throw new A1RepositoryException("Neispravan identifikator sacuvanog korisnika");
        }
        if (official.getUloga() != UlogaType.SLUZBENIK) {
            throw new AccessDeniedException("Pristup nije dozvoljen");
        }
        ResenjeType resolution = new ResenjeType();
        String requestUrl = documentBaseUrl + "/" + documentId;
        URI location = URI.create(requestUrl + "/resenje");
        resolution.setId(location.toString());
        resolution.setZahtevRef(requestUrl);
        resolution.setDatumOdluke(DatatypeFactory.newDefaultInstance()
                .newXMLGregorianCalendar(LocalDate.now().toString()));
        SluzbenikType identity = new SluzbenikType();
        identity.setId(official.getId());
        identity.setIme(official.getIme());
        identity.setPrezime(official.getPrezime());
        resolution.setSluzbenik(identity);
        if (decision.getOdobrenje() != null) {
            OdobrenjeType approval = new OdobrenjeType();
            approval.setSifraDela("AD-" + documentId);
            resolution.setOdobrenje(approval);
        } else {
            OdbijanjeType rejection = new OdbijanjeType();
            rejection.setObrazlozenje(decision.getOdbijanje().getObrazlozenje());
            resolution.setOdbijanje(rejection);
        }
        final String saved;
        try {
            saved = resolutionXml.toXml(resolution);
        } catch (JAXBException exception) {
            throw new IllegalStateException("Neuspesno generisanje resenja");
        }
        repository.saveIfAbsent(documentId, saved);
        notifyOwner(documentId, request.getVlasnikId(), saved);
        return new CreatedDocument(location, saved);
    }

    private void notifyOwner(String documentId, String ownerId, String savedXml) {
        if (ownerId == null || ownerId.isBlank()) {
            logger.info("Slanje mejla preskočeno za zahtev {}: dokument nema vlasnikId.", documentId);
            return;
        }
        try {
            var storedOwner = users.findById(ownerId);
            if (storedOwner.isEmpty()) {
                logger.warn("Slanje mejla preskočeno za zahtev {}: nalog vlasnika nije pronađen.", documentId);
                return;
            }
            KorisnikType owner = userXml.fromXml(storedOwner.get());
            if (!ownerId.equals(owner.getId())) {
                logger.warn("Slanje mejla preskočeno za zahtev {}: ID naloga ne odgovara vlasniku.", documentId);
                return;
            }
            byte[] pdf = resolutionPdf.toPdf(savedXml);
            emailService.sendResolution(owner.getEmail(), documentId, pdf);
            logger.info("Mejl sa PDF rešenjem poslat za zahtev {}.", documentId);
        } catch (Exception exception) {
            logger.warn("Slanje mejla sa PDF rešenjem nije uspelo za zahtev {} ({}). Rešenje je sačuvano.",
                    documentId, exception.getClass().getSimpleName());
        }
    }

    @Override
    public String findById(String documentId) {
        Authentication authentication = requireRole("ROLE_GRADJANIN", "ROLE_SLUZBENIK");
        ZahtevA1Type request = readRequest(documentId);
        boolean official = authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_SLUZBENIK".equals(authority.getAuthority()));
        if (!official && !authentication.getName().equals(request.getVlasnikId())) {
            throw new ResenjeNotFoundException();
        }
        String stored = repository.findById(documentId).orElseThrow(ResenjeNotFoundException::new);
        try {
            ResenjeType resolution = resolutionXml.fromXml(stored);
            String requestUrl = documentBaseUrl + "/" + documentId;
            if (!requestUrl.equals(resolution.getZahtevRef())
                    || !(requestUrl + "/resenje").equals(resolution.getId())) {
                throw new A1RepositoryException("Neispravna veza sacuvanog resenja");
            }
            return stored;
        } catch (JAXBException exception) {
            throw new A1RepositoryException("Neispravan sacuvani dokument resenja");
        }
    }

    private ZahtevA1Type readRequest(String documentId) {
        String stored = requests.findById(documentId).orElseThrow(ResenjeNotFoundException::new);
        try {
            return requestXml.fromXml(stored);
        } catch (JAXBException exception) {
            throw new A1RepositoryException("Neispravan sacuvani A-1 dokument");
        }
    }

    private Authentication requireRole(String... roles) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken
                || authentication.getName() == null || authentication.getName().isBlank()) {
            throw new AuthenticationCredentialsNotFoundException("Potrebna je prijava");
        }
        for (String role : roles) {
            if (authentication.getAuthorities().stream().anyMatch(authority -> role.equals(authority.getAuthority()))) {
                return authentication;
            }
        }
        throw new AccessDeniedException("Pristup nije dozvoljen");
    }
}
