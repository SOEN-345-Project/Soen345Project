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

import java.time.LocalDateTime;

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

    @Test
    void login_withUnknownEmail_returnsBadRequest() {
        LoginUserDto loginPayload = new LoginUserDto();
        loginPayload.setEmail("missing@example.com");
        loginPayload.setPassword("StrongPass123!");

        ResponseEntity<?> response = authenticationController.login(loginPayload);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(String.valueOf(response.getBody())).contains("User not found");
    }

    @Test
    void login_withUnverifiedUser_returnsBadRequest() {
        authenticationController.register(
                signupDto("unverified@example.com", "5551112222", "EMAIL")
        );

        LoginUserDto loginPayload = new LoginUserDto();
        loginPayload.setEmail("unverified@example.com");
        loginPayload.setPassword("StrongPass123!");

        ResponseEntity<?> response = authenticationController.login(loginPayload);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(String.valueOf(response.getBody())).contains("User not verified");
    }

    @Test
    void login_withWrongPassword_returnsBadRequest() {
        authenticationController.register(
                signupDto("wrongpass@example.com", "5551112222", "EMAIL")
        );
        verifyByStoredCode("wrongpass@example.com");

        LoginUserDto loginPayload = new LoginUserDto();
        loginPayload.setEmail("wrongpass@example.com");
        loginPayload.setPassword("WrongPass999!");

        ResponseEntity<?> response = authenticationController.login(loginPayload);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(String.valueOf(response.getBody())).contains("Bad credentials");
    }

    @Test
    void verify_withWrongCode_returnsBadRequest() {
        authenticationController.register(
                signupDto("wrongcode@example.com", "5551112222", "EMAIL")
        );

        VerifyUserDto verifyPayload = new VerifyUserDto();
        verifyPayload.setEmail("wrongcode@example.com");
        verifyPayload.setVerificationCode("000000");

        ResponseEntity<?> response = authenticationController.verify(verifyPayload);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(String.valueOf(response.getBody())).contains("Invalid verification code");
    }

    @Test
    void verify_withExpiredCode_returnsBadRequest() {
        authenticationController.register(
                signupDto("expired@example.com", "5551112222", "EMAIL")
        );
        User saved = userRepository.findByEmail("expired@example.com").orElseThrow();
        saved.setVerificationCodeExpiresAt(LocalDateTime.now().minusMinutes(1));
        userRepository.save(saved);

        VerifyUserDto verifyPayload = new VerifyUserDto();
        verifyPayload.setEmail("expired@example.com");
        verifyPayload.setVerificationCode(saved.getVerificationCode());

        ResponseEntity<?> response = authenticationController.verify(verifyPayload);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(String.valueOf(response.getBody())).contains("Verification code expired");
    }

    @Test
    void test_endpoint_returnsOk() {
        ResponseEntity<String> response = authenticationController.test();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("Authentication controller is working");
    }

    @Test
    void home_returnsWelcomeString() {
        String body = authenticationController.home();

        assertThat(body).contains("Welcome to Eventigo");
    }

    @Test
    void registerAdmin_createsAdministrator() {
        ResponseEntity<?> response = authenticationController.registerAdmin(
                signupDto("newadmin@example.com", "5551112222", "EMAIL")
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(userRepository.findByEmail("newadmin@example.com")).isPresent();
    }

    @Test
    void registerAdmin_duplicateEmail_returnsBadRequest() {
        authenticationController.registerAdmin(
                signupDto("dupadmin@example.com", "5551112222", "EMAIL")
        );

        ResponseEntity<?> response = authenticationController.registerAdmin(
                signupDto("dupadmin@example.com", "5551113333", "EMAIL")
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(String.valueOf(response.getBody())).contains("Email already in use");
    }

    @Test
    void resendVerification_forUnverifiedUser_returnsOk() {
        authenticationController.register(
                signupDto("resend@example.com", "5551112222", "EMAIL")
        );

        ResponseEntity<?> response = authenticationController.resendVerification("resend@example.com", null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(String.valueOf(response.getBody())).contains("resent successfully");
    }

    @Test
    void checkEmail_returnsFalseWhenMissing() {
        ResponseEntity<?> response = authenticationController.checkEmail("nobody@example.com");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(Boolean.FALSE);
    }

    @Test
    void signup_withPhoneVerification_sendsSmsPath() {
        // DB still requires a non-null email on User; PHONE only selects SMS for verification delivery.
        ResponseEntity<?> response = authenticationController.register(
                signupDto("phoneverify@example.com", "+15550001111", "PHONE")
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(userRepository.findByPhoneNumber("+15550001111")).isPresent();
    }

    @Test
    void resendVerification_forVerifiedUser_returnsBadRequest() {
        authenticationController.register(
                signupDto("already.verified@example.com", "5551112222", "EMAIL")
        );
        verifyByStoredCode("already.verified@example.com");

        ResponseEntity<?> response = authenticationController.resendVerification("already.verified@example.com", null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(String.valueOf(response.getBody())).contains("User already verified");
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

    private void verifyByStoredCode(String email) {
        User saved = userRepository.findByEmail(email).orElseThrow();
        VerifyUserDto verifyPayload = new VerifyUserDto();
        verifyPayload.setEmail(email);
        verifyPayload.setVerificationCode(saved.getVerificationCode());
        authenticationController.verify(verifyPayload);
    }
}
