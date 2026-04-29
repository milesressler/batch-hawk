package com.batchhawk;

import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.oauth2.jwt.JwtDecoder;

@TestConfiguration(proxyBeanMethods = false)
public class LocalDevSecurityConfig {

    @Bean
    @Primary
    JwtDecoder jwtDecoder() {
        return Mockito.mock(JwtDecoder.class);
    }
}
