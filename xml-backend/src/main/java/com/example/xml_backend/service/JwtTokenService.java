package com.example.xml_backend.service;

import java.time.Instant;
import com.example.xml_backend.model.korisnik.KorisnikType;

public interface JwtTokenService {
    IssuedToken issue(KorisnikType korisnik);

    record IssuedToken(String token, Instant expiresAt) {}
}
