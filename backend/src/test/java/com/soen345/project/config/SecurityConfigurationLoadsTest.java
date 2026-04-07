package com.soen345.project.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.cors.CorsConfigurationSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Loads {@link SecurityConfiguration} by using the {@code coverage} profile (not {@code test}),
 * since the security config is excluded under {@code @Profile("!test")}.
 */
@SpringBootTest
@ActiveProfiles("coverage")
class SecurityConfigurationLoadsTest {

    @Autowired
    private SecurityFilterChain securityFilterChain;

    @Autowired
    private CorsConfigurationSource corsConfigurationSource;

    @Test
    void securityFilterChainAndCorsAreWired() {
        assertThat(securityFilterChain).isNotNull();
        assertThat(corsConfigurationSource).isNotNull();
        assertThat(corsConfigurationSource.getCorsConfiguration(new MockHttpServletRequest())).isNotNull();
    }
}
