package com.soen345.project;

import com.soen345.project.controller.AuthenticationController;
import com.soen345.project.dto.LoginResponse;
import com.soen345.project.dto.LoginUserDto;
import com.soen345.project.dto.RegisterUserDto;
import com.soen345.project.dto.VerifyUserDto;
import com.soen345.project.model.User;
import com.soen345.project.repository.UserRepository;
import com.soen345.project.service.EmailService;
import com.soen345.project.service.SmsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class AuthenticationControllerIntegrationTest {

    @TestConfiguration
    static class TestOverrides {
        @Bean
        @Primary
        EmailService emailService() {
            return Mockito.mock(EmailService.class);
        }

        @Bean
        @Primary
        SmsService smsService() {
            return Mockito.mock(SmsService.class);
        }
    }

    @Autowired
    private AuthenticationController authenticationController;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void clearUsers() {
        userRepository.deleteAll();
    }

    @Test
    void signup_withEmail_createsUnverifiedUser() {
        ResponseEntity<?> response = authenticationController.register(
                signupDto("john@example.com", "5551112222", "EMAIL")
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(userRepository.findByEmail("john@example.com")).isPresent();
        assertThat(userRepository.findByEmail("john@example.com").orElseThrow().isEnabled()).isFalse();
    }

    @Test
    void signup_withDuplicateEmail_returnsBadRequest() {
        authenticationController.register(
                signupDto("duplicate@example.com", "5551112222", "EMAIL")
        );

        ResponseEntity<?> response = authenticationController.register(
                signupDto("duplicate@example.com", "5551113333", "EMAIL")
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(String.valueOf(response.getBody())).contains("Email already in use");
    }

    @Test
    void verify_withCorrectCode_enablesUser() {
        authenticationController.register(
                signupDto("verifyme@example.com", "5551112222", "EMAIL")
        );

        User saved = userRepository.findByEmail("verifyme@example.com").orElseThrow();
        VerifyUserDto verifyPayload = new VerifyUserDto();
        verifyPayload.setEmail("verifyme@example.com");
        verifyPayload.setVerificationCode(saved.getVerificationCode());

        ResponseEntity<?> response = authenticationController.verify(verifyPayload);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(String.valueOf(response.getBody())).contains("User verified successfully");
        assertThat(userRepository.findByEmail("verifyme@example.com").orElseThrow().isEnabled()).isTrue();
    }

    @Test
    void login_afterVerification_returnsJwtToken() {
        authenticationController.register(
                signupDto("loginme@example.com", "5551112222", "EMAIL")
        );

        User saved = userRepository.findByEmail("loginme@example.com").orElseThrow();
        saved.setEnabled(true);
        saved.setVerificationCode(null);
        saved.setVerificationCodeExpiresAt(null);
        userRepository.save(saved);

        LoginUserDto loginPayload = new LoginUserDto();
        loginPayload.setEmail("loginme@example.com");
        loginPayload.setPassword("StrongPass123!");

        ResponseEntity<?> response = authenticationController.login(loginPayload);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isInstanceOf(LoginResponse.class);
        LoginResponse body = (LoginResponse) response.getBody();
        assertThat(body.getToken()).isNotBlank();
        assertThat(body.getExpiresIn()).isGreaterThan(0);
    }

    @Test
    void checkEmail_returnsTrueWhenEmailExists() {
        authenticationController.register(
                signupDto("exists@example.com", "5551112222", "EMAIL")
        );

        ResponseEntity<?> response = authenticationController.checkEmail("exists@example.com");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(Boolean.TRUE);
    }

    private RegisterUserDto signupDto(String email, String phone, String method) {
        RegisterUserDto dto = new RegisterUserDto();
        dto.setFirstName("John");
        dto.setLastName("Doe");
        dto.setEmail(email);
        dto.setPassword("StrongPass123!");
        dto.setPhoneNumber(phone);
        dto.setVerificationMethod(method);
        return dto;
    }
}
