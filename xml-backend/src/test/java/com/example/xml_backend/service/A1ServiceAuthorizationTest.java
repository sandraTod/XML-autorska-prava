package com.example.xml_backend.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import com.example.xml_backend.repository.A1Repository;
import com.example.xml_backend.service.impl.A1ServiceImpl;

class A1ServiceAuthorizationTest {
    private final A1Repository repository = mock(A1Repository.class);
    private final A1XmlService xmlService = mock(A1XmlService.class);
    private final A1Service service = new A1ServiceImpl(xmlService, repository, mock(A1ListaXmlService.class), "https://example.org/a1");

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void missingAnonymousOrUnverifiedAuthenticationIsRejectedBeforeOperations() {
        SecurityContextHolder.clearContext();
        assertUnauthenticated();
        SecurityContextHolder.getContext().setAuthentication(new AnonymousAuthenticationToken("anonymous", "anonymousUser",
                List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))));
        assertUnauthenticated();
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.unauthenticated("https://example.org/nalozi/test", null));
        assertUnauthenticated();
        verifyNoInteractions(repository, xmlService);
    }

    private void assertUnauthenticated() {
        assertThrows(AuthenticationCredentialsNotFoundException.class, () -> service.create("<broken>"));
        assertThrows(AuthenticationCredentialsNotFoundException.class, () -> service.findById("test"));
        assertThrows(AuthenticationCredentialsNotFoundException.class, () -> service.findMyRequests());
    }

    @Test
    void wrongRolesCannotBypassServiceChecks() {
        SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
                "https://example.org/nalozi/test", null, List.of(new SimpleGrantedAuthority("ROLE_OTHER"))));
        assertThrows(AccessDeniedException.class, () -> service.create("<broken>"));
        assertThrows(AccessDeniedException.class, () -> service.findById("test"));
        assertThrows(AccessDeniedException.class, () -> service.findMyRequests());
        SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
                "https://example.org/nalozi/official", null, List.of(new SimpleGrantedAuthority("ROLE_SLUZBENIK"))));
        assertThrows(AccessDeniedException.class, () -> service.create("<broken>"));
        assertThrows(AccessDeniedException.class, () -> service.findMyRequests());
        verifyNoInteractions(repository, xmlService);
    }
}
