package com.example.xml_backend.service;

import java.util.Locale;

public final class EmailNormalizer {
    private EmailNormalizer() {}

    public static String normalize(String email) {
        if (email == null) {
            throw new IllegalArgumentException("E-mail je obavezan");
        }
        return email.strip().toLowerCase(Locale.ROOT);
    }
}
