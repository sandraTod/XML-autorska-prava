package com.example.xml_backend.service.impl;

import java.nio.charset.StandardCharsets;
import java.util.GregorianCalendar;
import java.time.ZoneOffset;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Service;
import com.example.xml_backend.model.korisnik.KorisnikType;
import com.example.xml_backend.model.prijava.PrijavaType;
import com.example.xml_backend.model.prijavaodgovor.PrijavaOdgovorType;
import com.example.xml_backend.repository.KorisnikRepository;
import com.example.xml_backend.service.EmailNormalizer;
import com.example.xml_backend.service.JwtTokenService;
import com.example.xml_backend.service.KorisnikXmlService;
import com.example.xml_backend.service.PrijavaOdgovorXmlService;
import com.example.xml_backend.service.PrijavaService;
import com.example.xml_backend.service.PrijavaXmlService;
import com.example.xml_backend.service.exception.InvalidCredentialsException;
import com.example.xml_backend.service.exception.InvalidPrijavaXmlException;
import jakarta.xml.bind.JAXBException;

@Service
public class PrijavaServiceImpl implements PrijavaService {
	
    private final PrijavaXmlService prijavaXmlService;
    private final PrijavaOdgovorXmlService odgovorXmlService;
    private final KorisnikXmlService korisnikXmlService;
    private final KorisnikRepository repository;
    private final JwtTokenService tokenService;
    private final String dummyHash = BCrypt.hashpw("dummy-nalog-nije-korisnik", BCrypt.gensalt(10));

    public PrijavaServiceImpl(PrijavaXmlService prijavaXmlService, PrijavaOdgovorXmlService odgovorXmlService,
            KorisnikXmlService korisnikXmlService, KorisnikRepository repository, JwtTokenService tokenService) {
        this.prijavaXmlService = prijavaXmlService;
        this.odgovorXmlService = odgovorXmlService;
        this.korisnikXmlService = korisnikXmlService;
        this.repository = repository;
        this.tokenService = tokenService;
    }

    @Override
    public String login(String xml) {
       
    	if (xml == null || xml.isBlank()) {
            throw new InvalidPrijavaXmlException();
        }
        final PrijavaType prijava;
        try {
            prijava = prijavaXmlService.fromXml(xml);
        } catch (JAXBException exception) {
            throw new InvalidPrijavaXmlException();
        }
        String email = EmailNormalizer.normalize(prijava.getEmail());
        String password = prijava.getLozinka();
        
        if (password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new InvalidCredentialsException();
        }
        var storedXml = repository.findByEmail(email);
        if (storedXml.isEmpty()) {
            BCrypt.checkpw(password, dummyHash);
            throw new InvalidCredentialsException();
        }
        final KorisnikType korisnik;
        try {
            korisnik = korisnikXmlService.fromXml(storedXml.get());
        } catch (JAXBException exception) {
            throw new IllegalStateException("Neispravan sačuvani nalog");
        }
        final boolean matches;
        try {
            matches = BCrypt.checkpw(password, korisnik.getLozinkaHash());
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException("Neispravan sačuvani nalog");
        }
        if (!matches) {
            throw new InvalidCredentialsException();
        }
        var issued = tokenService.issue(korisnik);
        var odgovor = new PrijavaOdgovorType();
        odgovor.setToken(issued.token());
        try {
            odgovor.setExpiresAt(DatatypeFactory.newInstance().newXMLGregorianCalendar(
                    GregorianCalendar.from(issued.expiresAt().atZone(ZoneOffset.UTC))));
            return odgovorXmlService.toXml(odgovor);
        } catch (DatatypeConfigurationException | JAXBException exception) {
            throw new IllegalStateException("Neuspešno kreiranje odgovora prijave");
        }
    }
}
