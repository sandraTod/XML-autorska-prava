package com.example.xml_backend.security;

import java.time.Clock;
import java.util.Base64;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import io.jsonwebtoken.security.Keys;

@Configuration
public class JwtConfiguration {
    @Bean
    public SecretKey jwtSigningKey(@Value("${jwt.secret-base64}") String encodedKey) {
        try {
            byte[] keyBytes = Base64.getDecoder().decode(encodedKey);
            if (keyBytes.length < 32) {
                throw new IllegalArgumentException();
            }
            return Keys.hmacShaKeyFor(keyBytes);
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException("jwt.secret-base64 mora sadržati Base64 ključ od najmanje 32 nasumična bajta.");
        }
    }

    @Bean
    public Clock jwtClock() {
        return Clock.systemUTC();
    }
}
