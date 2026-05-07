package com.batchhawk.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/actuator/health").permitAll()
                .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                .requestMatchers("/internal/**").hasAuthority("SCOPE_worker")
                .requestMatchers("/api/admin/**").hasRole("admin")
                .requestMatchers("/api/products/**").permitAll()
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())));

        return http.build();
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        final var scopeConverter = new JwtGrantedAuthoritiesConverter();
        final var converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            final var authorities = new ArrayList<GrantedAuthority>(scopeConverter.convert(jwt));
            @SuppressWarnings("unchecked")
            final var resourceAccess = (Map<String, Object>) jwt.getClaims().get("resource_access");
            if (resourceAccess != null) {
                @SuppressWarnings("unchecked")
                final var adminClient = (Map<String, Object>) resourceAccess.get("batch-hawk-admin");
                if (adminClient != null) {
                    @SuppressWarnings("unchecked")
                    final var roles = (List<String>) adminClient.get("roles");
                    if (roles != null) {
                        roles.stream()
                            .map(role -> (GrantedAuthority) new SimpleGrantedAuthority("ROLE_" + role))
                            .forEach(authorities::add);
                    }
                }
            }
            return authorities;
        });
        return converter;
    }
}