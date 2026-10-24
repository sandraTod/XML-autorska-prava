package com.example.xml_backend.service;

import java.time.Instant;
import com.example.xml_backend.model.korisnik.KorisnikType;
import com.example.xml_backend.model.korisnik.UlogaType;

public interface JwtTokenService {
    IssuedToken issue(KorisnikType korisnik);
    VerifiedToken verify(String token);

    record IssuedToken(String token, Instant expiresAt) {}
    record VerifiedToken(String subject, UlogaType uloga) {}
}
