package com.example.xml_backend.security;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class XmlSecurityErrorHandler implements AuthenticationEntryPoint, AccessDeniedHandler {
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException exception) throws IOException {
        response.setHeader("WWW-Authenticate", "Bearer");
        write(response, 401, "Potrebna je važeća prijava.");
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
            AccessDeniedException exception) throws IOException {
        write(response, 403, "Pristup nije dozvoljen.");
    }

    private void write(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("application/xml");
        response.setHeader("Cache-Control", "no-store");
        response.getWriter().write("<?xml version=\"1.0\" encoding=\"UTF-8\"?><greska><status>"
                + status + "</status><poruka>" + message + "</poruka></greska>");
    }
}
