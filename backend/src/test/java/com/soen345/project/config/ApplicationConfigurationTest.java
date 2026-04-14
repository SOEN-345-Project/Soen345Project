package com.soen345.project.config;

import com.soen345.project.model.Customer;
import com.soen345.project.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApplicationConfigurationTest {

    @Mock
    private UserRepository userRepository;

    private ApplicationConfiguration configuration;

    @BeforeEach
    void setUp() {
        configuration = new ApplicationConfiguration(userRepository);
    }

    @Test
    void userDetailsService_loadsExistingUser() {
        Customer c = new Customer();
        c.setEmail("a@b.com");
        when(userRepository.findByEmail("a@b.com")).thenReturn(Optional.of(c));

        UserDetailsService uds = configuration.userDetailsService();
        assertThat(uds.loadUserByUsername("a@b.com")).isSameAs(c);
    }

    @Test
    void userDetailsService_throwsWhenMissing() {
        when(userRepository.findByEmail("x@y.com")).thenReturn(Optional.empty());

        UserDetailsService uds = configuration.userDetailsService();
        assertThatThrownBy(() -> uds.loadUserByUsername("x@y.com"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("User not found");
    }

    @Test
    void passwordEncoder_isBcrypt() {
        PasswordEncoder encoder = configuration.passwordEncoder();
        String hash = encoder.encode("secret");
        assertThat(encoder.matches("secret", hash)).isTrue();
    }
}
