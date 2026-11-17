package com.example.xml_backend.service;

import java.nio.charset.StandardCharsets;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Service
public class EmailService {
    private final JavaMailSender mailSender;
    private final String from;

    public EmailService(JavaMailSender mailSender, @Value("${app.mail.from}") String from) {
        this.mailSender = mailSender;
        this.from = from;
    }

    public void sendResolution(String to, String documentId, byte[] pdf) throws MessagingException {
        if (from.isBlank()) {
            throw new IllegalStateException("Nije podešena adresa pošiljaoca");
        }
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
        helper.setFrom(from);
        helper.setTo(to);
        helper.setSubject("Doneto je rešenje o A-1 zahtevu");
        helper.setText("Poštovani,\n\nu prilogu se nalazi rešenje o Vašem A-1 zahtevu.", false);
        helper.addAttachment("resenje-" + documentId + ".pdf", new ByteArrayResource(pdf), "application/pdf");
        mailSender.send(message);
    }
}
