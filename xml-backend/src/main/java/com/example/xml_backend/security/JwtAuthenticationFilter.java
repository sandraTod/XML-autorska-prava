package com.example.xml_backend.security;

import java.io.IOException;
import java.util.List;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import com.example.xml_backend.service.JwtTokenService;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class JwtAuthenticationFilter extends OncePerRequestFilter {
	
    private final JwtTokenService tokens;
    private final XmlSecurityErrorHandler errors;

    public JwtAuthenticationFilter(JwtTokenService tokens, XmlSecurityErrorHandler errors) {
        this.tokens = tokens;
        this.errors = errors;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain chain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header == null) {
            chain.doFilter(request, response);
            return;
        }
        try {
            var headers = request.getHeaders("Authorization");
            headers.nextElement();
            if (headers.hasMoreElements() || !header.regionMatches(true, 0, "Bearer ", 0, 7)) {
                throw new BadCredentialsException("Neispravno zaglavlje");
            }
            var verified = tokens.verify(header.substring(7));
            var authentication = UsernamePasswordAuthenticationToken.authenticated(verified.subject(), null,
                    List.of(new SimpleGrantedAuthority("ROLE_" + verified.uloga().value())));
            var context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);
        } catch (JwtException | IllegalArgumentException | BadCredentialsException exception) {
            SecurityContextHolder.clearContext();
            errors.commence(request, response, new BadCredentialsException("Neispravan token"));
            return;
        }
        chain.doFilter(request, response);
    }
}
