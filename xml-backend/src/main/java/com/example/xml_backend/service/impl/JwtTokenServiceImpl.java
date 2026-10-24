package com.example.xml_backend.service.impl;

import java.time.Clock;
import java.net.URI;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;
import com.example.xml_backend.model.korisnik.KorisnikType;
import com.example.xml_backend.model.korisnik.UlogaType;
import com.example.xml_backend.service.JwtTokenService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.JwtParser;

@Service
public class JwtTokenServiceImpl implements JwtTokenService {
    private final SecretKey signingKey;
    private final Clock clock;
    private final JwtParser parser;

    public JwtTokenServiceImpl(SecretKey signingKey, Clock clock) {
        this.signingKey = signingKey;
        this.clock = clock;
        this.parser = Jwts.parser().verifyWith(signingKey)
                .sig().clear().add(Jwts.SIG.HS256).and()
                .clock(() -> Date.from(clock.instant())).build();
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

    @Override
    public VerifiedToken verify(String token) {
        var signed = parser.parseSignedClaims(token);
        var claims = signed.getPayload();
        if (!"HS256".equals(signed.getHeader().getAlgorithm()) || claims.getExpiration() == null
                || !claims.getExpiration().toInstant().isAfter(clock.instant())) {
            throw new JwtException("Neispravan token");
        }
        String subject = claims.getSubject();
        if (subject == null || subject.isBlank()) {
            throw new JwtException("Neispravan token");
        }
        URI userId = URI.create(subject);
        if (!("http".equals(userId.getScheme()) || "https".equals(userId.getScheme()))
                || userId.getHost() == null || userId.getUserInfo() != null
                || userId.getPath() == null || userId.getPath().length() < 2
                || userId.getQuery() != null || userId.getFragment() != null) {
            throw new JwtException("Neispravan token");
        }
        String role = claims.get("uloga", String.class);
        if (role == null) {
            throw new JwtException("Neispravan token");
        }
        return new VerifiedToken(subject, UlogaType.fromValue(role));
    }
}
