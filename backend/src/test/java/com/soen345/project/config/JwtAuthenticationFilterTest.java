package com.soen345.project.config;

import com.soen345.project.model.Customer;
import com.soen345.project.service.JwtService;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtService jwtService;
    @Mock
    private UserDetailsService userDetailsService;
    @Mock
    private FilterChain filterChain;

    private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        filter = new JwtAuthenticationFilter(jwtService, userDetailsService);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void doFilterInternal_noHeader_continuesChain() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setRequestURI("/api/events");
        MockHttpServletResponse res = new MockHttpServletResponse();

        filter.doFilterInternal(req, res, filterChain);

        verify(filterChain).doFilter(req, res);
        verify(jwtService, never()).extractUsername(any());
    }

    @Test
    void doFilterInternal_invalidJwt_illegalArgument_returns401() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader("Authorization", "Bearer x");
        MockHttpServletResponse res = new MockHttpServletResponse();
        when(jwtService.extractUsername("x")).thenThrow(new IllegalArgumentException("x"));

        filter.doFilterInternal(req, res, filterChain);

        assertThat(res.getStatus()).isEqualTo(401);
        verify(filterChain, never()).doFilter(req, res);
    }

    @Test
    void doFilterInternal_validJwt_setsAuthentication() throws Exception {
        Customer user = new Customer();
        user.setEmail("u@example.com");
        user.setPassword("p");
        user.setFirstName("A");
        user.setLastName("B");
        user.setEnabled(true);

        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader("Authorization", "Bearer token");
        MockHttpServletResponse res = new MockHttpServletResponse();

        when(jwtService.extractUsername("token")).thenReturn("u@example.com");
        when(userDetailsService.loadUserByUsername("u@example.com")).thenReturn(user);
        when(jwtService.isTokenValid(eq("token"), any(UserDetails.class))).thenReturn(true);

        filter.doFilterInternal(req, res, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getName()).isEqualTo("u@example.com");
        verify(filterChain).doFilter(req, res);
    }

    @Test
    void doFilterInternal_expiredJwt_invalidToken_returns401() throws Exception {
        Customer user = new Customer();
        user.setEmail("u@example.com");
        user.setPassword("p");
        user.setFirstName("A");
        user.setLastName("B");
        user.setEnabled(true);

        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader("Authorization", "Bearer token");
        MockHttpServletResponse res = new MockHttpServletResponse();

        when(jwtService.extractUsername("token")).thenReturn("u@example.com");
        when(userDetailsService.loadUserByUsername("u@example.com")).thenReturn(user);
        when(jwtService.isTokenValid(eq("token"), any(UserDetails.class))).thenReturn(false);

        filter.doFilterInternal(req, res, filterChain);

        assertThat(res.getStatus()).isEqualTo(401);
        assertThat(res.getContentAsString()).contains("Invalid token");
        verify(filterChain, never()).doFilter(req, res);
    }
}
