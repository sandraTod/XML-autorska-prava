package com.example.xml_backend.service.impl;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Service;
import com.example.xml_backend.model.korisnik.KorisnikType;
import com.example.xml_backend.model.korisnik.UlogaType;
import com.example.xml_backend.model.registracija.RegistracijaType;
import com.example.xml_backend.repository.KorisnikRepository;
import com.example.xml_backend.service.EmailNormalizer;
import com.example.xml_backend.service.KorisnikXmlService;
import com.example.xml_backend.service.RegistracijaService;
import com.example.xml_backend.service.RegistracijaXmlService;
import jakarta.xml.bind.JAXBException;

@Service
public class RegistracijaServiceImpl implements RegistracijaService {
	
    private static final Pattern EMAIL = Pattern.compile("[A-Za-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.[A-Za-z0-9!#$%&'*+/=?^_`{|}~-]+)*@(?:[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?\\.)+[A-Za-z]{2,63}");
    private final RegistracijaXmlService registrationXmlService;
    private final KorisnikXmlService userXmlService;
    private final KorisnikRepository repository;
    private final String accountBaseUrl;

    public RegistracijaServiceImpl(RegistracijaXmlService registrationXmlService,
            KorisnikXmlService userXmlService, KorisnikRepository repository,
            @Value("${korisnik.document-base-url}") String accountBaseUrl) {
        this.registrationXmlService = registrationXmlService;
        this.userXmlService = userXmlService;
        this.repository = repository;
        URI base = URI.create(accountBaseUrl);
        if (!("http".equals(base.getScheme()) || "https".equals(base.getScheme()))
                || base.getHost() == null || base.getUserInfo() != null || base.getQuery() != null || base.getFragment() != null) {
            throw new IllegalArgumentException("Neispravan URL naloga");
        }
        this.accountBaseUrl = accountBaseUrl.replaceAll("/+$", "");
    }

    @Override
    public void register(String xml) {
        if (xml == null || xml.isBlank()) {
            throw new IllegalArgumentException("Neispravna registracija");
        }
        final RegistracijaType registration;
        try {
            registration = registrationXmlService.fromXml(xml);
        } catch (JAXBException exception) {
            throw new IllegalArgumentException("Neispravna registracija");
        }
        String email = EmailNormalizer.normalize(registration.getEmail());
        String password = registration.getLozinka();
        if (email.length() > 254 || email.indexOf('@') > 64
                || !EMAIL.matcher(email).matches() || password == null
                || password.codePointCount(0, password.length()) < 8
                || password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new IllegalArgumentException("Neispravna registracija");
        }
        KorisnikType user = new KorisnikType();
        user.setId(accountBaseUrl + "/" + UUID.randomUUID());
        user.setIme(registration.getIme());
        user.setPrezime(registration.getPrezime());
        user.setEmail(email);
        user.setUloga(UlogaType.GRADJANIN);
        user.setLozinkaHash(BCrypt.hashpw(password, BCrypt.gensalt(10)));
        final String userXml;
        try {
            userXml = userXmlService.toXml(user);
        } catch (JAXBException exception) {
            throw new IllegalStateException("Neuspešno kreiranje naloga");
        }
        repository.save(email, userXml);
    }
}
