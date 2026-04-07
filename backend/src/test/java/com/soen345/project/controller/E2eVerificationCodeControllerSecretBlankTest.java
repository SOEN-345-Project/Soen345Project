package com.soen345.project.controller;

import com.soen345.project.repository.UserRepository;
import com.soen345.project.service.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = E2eVerificationCodeController.class)
@AutoConfigureMockMvc(addFilters = false)
@TestPropertySource(
        properties = {
                "e2e.expose-verification-code=true",
                "e2e.verification-secret="
        })
class E2eVerificationCodeControllerSecretBlankTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    @SuppressWarnings("unused")
    private UserRepository userRepository;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @Test
    void getVerificationCode_forbiddenWhenSecretNotConfigured() throws Exception {
        mockMvc.perform(get("/auth/e2e/verification-code").param("email", "a@b.com"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error", is("E2E secret not configured")));
    }
}
