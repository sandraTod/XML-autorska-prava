package com.example.xml_backend.service.impl;

import java.time.Clock;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;
import com.example.xml_backend.model.korisnik.KorisnikType;
import com.example.xml_backend.service.JwtTokenService;
import io.jsonwebtoken.Jwts;

@Service
public class JwtTokenServiceImpl implements JwtTokenService {
    private final SecretKey signingKey;
    private final Clock clock;

    public JwtTokenServiceImpl(SecretKey signingKey, Clock clock) {
        this.signingKey = signingKey;
        this.clock = clock;
    }

    @Override
    public IssuedToken issue(KorisnikType korisnik) {
        var issuedAt = clock.instant().truncatedTo(ChronoUnit.SECONDS);
        var expiresAt = issuedAt.plus(Duration.ofHours(24));
        String token = Jwts.builder().subject(korisnik.getId())
                .claim("uloga", korisnik.getUloga().value())
                .issuedAt(Date.from(issuedAt)).expiration(Date.from(expiresAt))
                .signWith(signingKey, Jwts.SIG.HS256).compact();
        return new IssuedToken(token, expiresAt);
    }
}
