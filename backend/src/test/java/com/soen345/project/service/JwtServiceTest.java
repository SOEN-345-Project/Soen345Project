package com.soen345.project.service;

import com.soen345.project.model.Customer;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Encoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.SecretKey;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        SecretKey key = Keys.secretKeyFor(SignatureAlgorithm.HS256);
        ReflectionTestUtils.setField(jwtService, "secretKey", Encoders.BASE64.encode(key.getEncoded()));
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", 3_600_000L);
    }

    @Test
    void getExpirationTime_returnsConfiguredValue() {
        assertThat(jwtService.getExpirationTime()).isEqualTo(3_600_000L);
    }

    @Test
    void generateToken_andExtractUsername_forCustomerUsesEmailAsSubject() {
        Customer user = new Customer();
        user.setId(42L);
        user.setEmail("user@example.com");
        user.setPassword("secret");
        user.setFirstName("A");
        user.setLastName("B");
        user.setEnabled(true);

        String token = jwtService.generateToken(user);

        assertThat(jwtService.extractUsername(token)).isEqualTo("user@example.com");
    }

    @Test
    void isTokenValid_returnsTrueForMatchingUser() {
        Customer user = new Customer();
        user.setId(1L);
        user.setEmail("match@example.com");
        user.setPassword("p");
        user.setFirstName("A");
        user.setLastName("B");
        user.setEnabled(true);

        String token = jwtService.generateToken(user);

        assertThat(jwtService.isTokenValid(token, user)).isTrue();
    }

    @Test
    void isTokenValid_returnsFalseWhenUsernameDiffers() {
        Customer signedUser = new Customer();
        signedUser.setId(1L);
        signedUser.setEmail("a@example.com");
        signedUser.setPassword("p");
        signedUser.setFirstName("A");
        signedUser.setLastName("B");
        signedUser.setEnabled(true);

        Customer otherUser = new Customer();
        otherUser.setId(2L);
        otherUser.setEmail("b@example.com");
        otherUser.setPassword("p");
        otherUser.setFirstName("C");
        otherUser.setLastName("D");
        otherUser.setEnabled(true);

        String token = jwtService.generateToken(signedUser);

        assertThat(jwtService.isTokenValid(token, otherUser)).isFalse();
    }

    @Test
    void generateToken_usesSpringSecurityUsernameWhenNotProjectUser() {
        UserDetails generic = User.builder()
                .username("generic-subject")
                .password("p")
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_USER")))
                .build();

        String token = jwtService.generateToken(generic);

        assertThat(jwtService.extractUsername(token)).isEqualTo("generic-subject");
    }
}
