package com.soen345.project.controller;

import com.soen345.project.model.Customer;
import com.soen345.project.repository.UserRepository;
import com.soen345.project.service.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = E2eVerificationCodeController.class)
@AutoConfigureMockMvc(addFilters = false)
@TestPropertySource(
        properties = {
                "e2e.expose-verification-code=true",
                "e2e.verification-secret=test-secret-123"
        })
class E2eVerificationCodeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @Test
    void getVerificationCode_forbiddenWhenSecretHeaderWrong() throws Exception {
        mockMvc.perform(get("/auth/e2e/verification-code")
                        .param("email", "a@b.com")
                        .header("X-E2E-Secret", "wrong"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error", is("Forbidden")));
    }

    @Test
    void getVerificationCode_forbiddenWhenSecretMissing() throws Exception {
        mockMvc.perform(get("/auth/e2e/verification-code").param("email", "a@b.com"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getVerificationCode_returnsCodeWhenPendingUser() throws Exception {
        Customer c = new Customer();
        c.setEmail("a@b.com");
        c.setVerificationCode("777888");
        c.setEnabled(false);
        when(userRepository.findByEmail("a@b.com")).thenReturn(Optional.of(c));

        mockMvc.perform(get("/auth/e2e/verification-code")
                        .param("email", "a@b.com")
                        .header("X-E2E-Secret", "test-secret-123")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verificationCode", is("777888")));
    }

    @Test
    void getVerificationCode_notFoundWhenUserAlreadyEnabled() throws Exception {
        Customer c = new Customer();
        c.setEmail("a@b.com");
        c.setVerificationCode("777888");
        c.setEnabled(true);
        when(userRepository.findByEmail("a@b.com")).thenReturn(Optional.of(c));

        mockMvc.perform(get("/auth/e2e/verification-code")
                        .param("email", "a@b.com")
                        .header("X-E2E-Secret", "test-secret-123"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getVerificationCode_notFoundWhenNoPendingCode() throws Exception {
        Customer c = new Customer();
        c.setEmail("a@b.com");
        c.setEnabled(false);
        c.setVerificationCode(" ");
        when(userRepository.findByEmail("a@b.com")).thenReturn(Optional.of(c));

        mockMvc.perform(get("/auth/e2e/verification-code")
                        .param("email", "a@b.com")
                        .header("X-E2E-Secret", "test-secret-123"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getVerificationCode_notFoundWhenEmailUnknown() throws Exception {
        when(userRepository.findByEmail("missing@b.com")).thenReturn(Optional.empty());

        mockMvc.perform(get("/auth/e2e/verification-code")
                        .param("email", "missing@b.com")
                        .header("X-E2E-Secret", "test-secret-123"))
                .andExpect(status().isNotFound());
    }
}
