package com.soen345.project.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.soen345.project.dto.LoginUserDto;
import com.soen345.project.dto.RegisterUserDto;
import com.soen345.project.dto.VerifyUserDto;
import com.soen345.project.model.Administrator;
import com.soen345.project.model.Customer;
import com.soen345.project.service.AuthenticationService;
import com.soen345.project.service.JwtService;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
    void signup_checkedException_isWrappedInRuntimeException() throws Exception {
        RegisterUserDto dto = new RegisterUserDto();
        dto.setFirstName("A");
        dto.setLastName("B");
        dto.setEmail("x@b.com");
        dto.setPassword("p");
        when(authenticationService.signup(any(RegisterUserDto.class))).thenThrow(new Exception("SMTP failure"));

        ServletException thrown = assertThrows(ServletException.class, () ->
                mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto))));

        assertThat(thrown.getCause()).isInstanceOf(RuntimeException.class);
        assertThat(thrown.getCause().getCause()).isInstanceOf(Exception.class);
    }

    @Test
    void signup_ok_returnsUser() throws Exception {
        RegisterUserDto dto = new RegisterUserDto();
        dto.setFirstName("A");
        dto.setLastName("B");
        dto.setEmail("new@b.com");
        dto.setPassword("p");
        dto.setVerificationMethod("EMAIL");
        Customer created = new Customer();
        created.setId(50L);
        created.setEmail("new@b.com");
        when(authenticationService.signup(any(RegisterUserDto.class))).thenReturn(created);

        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("new@b.com"));
    }

    @Test
    void signupAdmin_returnsAdmin() throws Exception {
        RegisterUserDto dto = new RegisterUserDto();
        dto.setFirstName("Admin");
        dto.setLastName("User");
        dto.setEmail("newadmin@x.com");
        dto.setPassword("secret");
        Administrator admin = new Administrator();
        admin.setId(88L);
        admin.setEmail("newadmin@x.com");
        when(authenticationService.createAdministrator(any(RegisterUserDto.class))).thenReturn(admin);

        mockMvc.perform(post("/auth/signup/admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("newadmin@x.com"))
                .andExpect(jsonPath("$.id").value(88));
    }

    @Test
    void signupAdmin_badRequest_returnsMessage() throws Exception {
        RegisterUserDto dto = new RegisterUserDto();
        dto.setEmail("taken@x.com");
        when(authenticationService.createAdministrator(any(RegisterUserDto.class)))
                .thenThrow(new RuntimeException("Email already in use"));

        mockMvc.perform(post("/auth/signup/admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Email already in use"));
    }

    @Test
    void resendVerification_withEmail_ok() throws Exception {
        doNothing().when(authenticationService).resendVerificationCode(eq("u@example.com"), isNull());

        mockMvc.perform(post("/auth/resend").param("email", "u@example.com"))
                .andExpect(status().isOk())
                .andExpect(content().string("Verification code resent successfully"));

        verify(authenticationService).resendVerificationCode("u@example.com", null);
    }

    @Test
    void resendVerification_withPhone_ok() throws Exception {
        doNothing().when(authenticationService).resendVerificationCode(isNull(), eq("+15550001"));

        mockMvc.perform(post("/auth/resend").param("phoneNumber", "+15550001"))
                .andExpect(status().isOk())
                .andExpect(content().string("Verification code resent successfully"));

        verify(authenticationService).resendVerificationCode(null, "+15550001");
    }

    @Test
    void resendVerification_badRequest_returnsMessage() throws Exception {
        doThrow(new RuntimeException("User not found"))
                .when(authenticationService)
                .resendVerificationCode(eq("nope@x.com"), isNull());

        mockMvc.perform(post("/auth/resend").param("email", "nope@x.com"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("User not found"));
    }

    @Test
    void home_returnsWelcome() throws Exception {
        mockMvc.perform(get("/auth/"))
                .andExpect(status().isOk())
                .andExpect(content().string("Welcome to Eventigo!"));
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
    void verify_badRequest_returnsMessage() throws Exception {
        VerifyUserDto dto = new VerifyUserDto();
        dto.setEmail("a@b.com");
        dto.setVerificationCode("wrong");
        doThrow(new RuntimeException("Invalid verification code"))
                .when(authenticationService)
                .verifyUser(any(VerifyUserDto.class));

        mockMvc.perform(post("/auth/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Invalid verification code"));
    }

    @Test
    void checkEmail_returnsBoolean() throws Exception {
        when(authenticationService.emailExists("x@y.com")).thenReturn(true);

        mockMvc.perform(get("/auth/check-email").param("email", "x@y.com"))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
    }

    @Test
    void checkEmail_returnsFalse_whenMissing() throws Exception {
        when(authenticationService.emailExists("free@y.com")).thenReturn(false);

        mockMvc.perform(get("/auth/check-email").param("email", "free@y.com"))
                .andExpect(status().isOk())
                .andExpect(content().string("false"));
    }

    @Test
    void testEndpoint() throws Exception {
        mockMvc.perform(get("/auth/test"))
                .andExpect(status().isOk())
                .andExpect(content().string("Authentication controller is working!"));
    }
}
