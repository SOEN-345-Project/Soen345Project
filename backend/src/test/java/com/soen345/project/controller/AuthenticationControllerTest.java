package com.soen345.project.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.soen345.project.dto.LoginUserDto;
import com.soen345.project.dto.RegisterUserDto;
import com.soen345.project.dto.VerifyUserDto;
import com.soen345.project.model.Customer;
import com.soen345.project.service.AuthenticationService;
import com.soen345.project.service.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AuthenticationController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthenticationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private AuthenticationService authenticationService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @Test
    void login_returnsToken() throws Exception {
        LoginUserDto dto = new LoginUserDto();
        dto.setEmail("a@b.com");
        dto.setPassword("secret");

        Customer user = new Customer();
        user.setId(1L);
        user.setEmail("a@b.com");
        when(authenticationService.authenticate(any(LoginUserDto.class))).thenReturn(user);
        when(jwtService.generateToken(user)).thenReturn("jwt-token");
        when(jwtService.getExpirationTime()).thenReturn(3600L);

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token"))
                .andExpect(jsonPath("$.expiresIn").value(3600));
    }

    @Test
    void login_badRequest_returnsMessage() throws Exception {
        LoginUserDto dto = new LoginUserDto();
        dto.setEmail("a@b.com");
        dto.setPassword("wrong");
        when(authenticationService.authenticate(any(LoginUserDto.class)))
                .thenThrow(new RuntimeException("Invalid credentials"));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Invalid credentials"));
    }

    @Test
    void signup_badRequest_returnsMessage() throws Exception {
        RegisterUserDto dto = new RegisterUserDto();
        dto.setFirstName("A");
        dto.setLastName("B");
        dto.setEmail("a@b.com");
        dto.setPassword("p");
        when(authenticationService.signup(any(RegisterUserDto.class)))
                .thenThrow(new RuntimeException("Email already in use"));

        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Email already in use"));
    }

    @Test
    void verify_ok() throws Exception {
        VerifyUserDto dto = new VerifyUserDto();
        dto.setEmail("a@b.com");
        dto.setVerificationCode("123456");
        doNothing().when(authenticationService).verifyUser(any(VerifyUserDto.class));

        mockMvc.perform(post("/auth/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(content().string("User verified successfully"));
    }

    @Test
    void checkEmail_returnsBoolean() throws Exception {
        when(authenticationService.emailExists("x@y.com")).thenReturn(true);

        mockMvc.perform(get("/auth/check-email").param("email", "x@y.com"))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
    }

    @Test
    void testEndpoint() throws Exception {
        mockMvc.perform(get("/auth/test"))
                .andExpect(status().isOk())
                .andExpect(content().string("Authentication controller is working!"));
    }
}
