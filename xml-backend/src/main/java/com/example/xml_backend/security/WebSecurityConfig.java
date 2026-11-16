package com.example.xml_backend.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import com.example.xml_backend.service.JwtTokenService;

@Configuration
public class WebSecurityConfig {
	
    private final JwtTokenService tokens;
    private final XmlSecurityErrorHandler errors;

    public WebSecurityConfig(JwtTokenService tokens, XmlSecurityErrorHandler errors) {
        this.tokens = tokens;
        this.errors = errors;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http.csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .requestCache(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .exceptionHandling(exceptions -> exceptions.authenticationEntryPoint(errors).accessDeniedHandler(errors))
                .authorizeHttpRequests(routes -> routes
                        .requestMatchers(HttpMethod.POST, "/api/auth/registracija", "/api/auth/prijava").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/sluzbenik/a1/zahtevi/{documentId}/resenje").hasRole("SLUZBENIK")
                        .requestMatchers(HttpMethod.GET, "/api/a1/zahtevi/{documentId}/resenje").hasAnyRole("GRADJANIN", "SLUZBENIK")
                        .requestMatchers(HttpMethod.GET, "/api/a1/zahtevi/{documentId}/resenje/xhtml").hasAnyRole("GRADJANIN", "SLUZBENIK")
                        .requestMatchers(HttpMethod.GET, "/api/a1/zahtevi/{documentId}/resenje/pdf").hasAnyRole("GRADJANIN", "SLUZBENIK")
                        .requestMatchers(HttpMethod.GET, "/api/a1/zahtevi/{documentId}/xhtml").hasAnyRole("GRADJANIN", "SLUZBENIK")
                        .requestMatchers(HttpMethod.GET, "/api/sluzbenik/a1/zahtevi").hasRole("SLUZBENIK")
                        .requestMatchers(HttpMethod.GET, "/api/sluzbenik/a1/zahtevi/pretraga").hasRole("SLUZBENIK")
                        .requestMatchers(HttpMethod.POST, "/api/a1/zahtevi").hasRole("GRADJANIN")
                        .requestMatchers(HttpMethod.GET, "/api/a1/zahtevi/listaZahteva").hasRole("GRADJANIN")
                        .requestMatchers(HttpMethod.GET, "/api/a1/zavedena-dela").hasAnyRole("GRADJANIN", "SLUZBENIK")
                        .requestMatchers(HttpMethod.GET, "/api/a1/zahtevi/{documentId}").hasAnyRole("GRADJANIN", "SLUZBENIK")
                        .anyRequest().denyAll())
                .addFilterBefore(new JwtAuthenticationFilter(tokens, errors), UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}
